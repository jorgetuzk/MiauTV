"""
TV-specific API endpoints optimized for Android TV clients.
"""
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc, func
from sqlalchemy.orm import selectinload

from ..database import get_db
from ..models import File, User, Folder, WatchProgress, ContentType
from ..auth import get_current_user
from ..config import get_settings
from ..schemas import FolderResponse, CategoryOverviewFolder
from ..services import (
    escape_like,
    add_urls_to_file,
    add_urls_to_folder,
    fetch_recent_files,
    fetch_continue_watching_files
)

router = APIRouter(prefix="/tv", tags=["TV"])
settings = get_settings()


async def _resolve_midia_folder_ids(db: AsyncSession, user_id: int) -> List[int]:
    """The user's Mídia root folder(s) — top-level (parent_id IS NULL)
    folders carrying the Mídia content_type_id, whose direct children are
    the subtype folders (Filmes/Séries/Animes/Hot/...).

    content_type_id=Mídia isn't unique to this root folder — it's also
    inherited/set on individual titles and files deep in the tree (e.g. a
    show's own folder, or files tagged via TMDb) — so without the
    parent_id IS NULL filter this would match dozens of unrelated nested
    folders too, and every query built on top of it (subtype pills,
    Destaques/Recentes pooling, search scoping) would silently include
    their entire subtrees. Mirrors content_types.py's get_category_overview,
    which resolves the same root folder the same way."""
    midia_type = await db.execute(
        select(ContentType.id).where(
            ContentType.user_id == user_id,
            ContentType.parent_id.is_(None),
            ContentType.slug == "midia",
        )
    )
    midia_type_id = midia_type.scalar_one_or_none()
    if midia_type_id is None:
        return []

    midia_folders = await db.execute(
        select(Folder.id).where(
            Folder.user_id == user_id,
            Folder.content_type_id == midia_type_id,
            Folder.parent_id.is_(None),
        )
    )
    return list(midia_folders.scalars().all())


async def _resolve_midia_type_tree_ids(db: AsyncSession, user_id: int) -> List[int]:
    """Mídia's own content-type id plus its direct children's (Filmes/
    Séries/Animes/...) — used to tell a subtype folder that's still
    genuinely part of Mídia from one that's been retagged to a different
    top-level type (e.g. "xHot" edited to carry the Hot type) via "Editar
    Info", even though it still physically sits inside the Mídia folder."""
    midia_type = await db.execute(
        select(ContentType.id).where(
            ContentType.user_id == user_id,
            ContentType.parent_id.is_(None),
            ContentType.slug == "midia",
        )
    )
    midia_type_id = midia_type.scalar_one_or_none()
    if midia_type_id is None:
        return []

    children = await db.execute(
        select(ContentType.id).where(ContentType.user_id == user_id, ContentType.parent_id == midia_type_id)
    )
    return [midia_type_id] + list(children.scalars().all())


async def _resolve_hidden_subtype_ids(db: AsyncSession, user_id: int, midia_folder_ids: List[int]) -> List[int]:
    """Subtype folders (direct children of the Mídia root) whose own
    content_type_id has been set to something outside Mídia's type tree —
    they still show up as a Menu 1 pill (so they're reachable), but their
    content stays out of the pooled "Visão geral" overview, Continue
    Watching and search until that specific pill is selected."""
    if not midia_folder_ids:
        return []
    midia_type_tree_ids = await _resolve_midia_type_tree_ids(db, user_id)
    if not midia_type_tree_ids:
        return []

    hidden = await db.execute(
        select(Folder.id).where(
            Folder.user_id == user_id,
            Folder.parent_id.in_(midia_folder_ids),
            Folder.content_type_id.is_not(None),
            Folder.content_type_id.notin_(midia_type_tree_ids),
        )
    )
    return list(hidden.scalars().all())


async def _build_subtype_pills(db: AsyncSession, root_ids: List[int]) -> List[CategoryOverviewFolder]:
    """Direct children of `root_ids` (the Mídia root folder(s)) — the
    category pill menu itself (Séries/Filmes/Animes/Hot). item_count is how
    many TITLE folders (movies/shows) sit directly inside each one — folders,
    not files. (The web app's own quick-nav pill bar, content_types.py's
    _build_subtype_entries, is deliberately file-counted instead — this is a
    TV-specific choice, since a folder count is what actually matches what
    picking that pill scopes Destaques/Recentes down to.)"""
    if not root_ids:
        return []

    from .folders import _auto_cover_url

    result = await db.execute(
        select(Folder).where(Folder.parent_id.in_(root_ids)).order_by(Folder.name)
    )
    subfolders = result.scalars().all()
    if not subfolders:
        return []

    count_rows = await db.execute(
        select(Folder.parent_id, func.count(Folder.id))
        .where(Folder.parent_id.in_([f.id for f in subfolders]))
        .group_by(Folder.parent_id)
    )
    title_counts = dict(count_rows.all())

    entries = []
    for subfolder in subfolders:
        item_count = title_counts.get(subfolder.id, 0)
        auto_cover_url = None if subfolder.custom_thumbnail_file_id else await _auto_cover_url(db, subfolder.id)
        folder_response = FolderResponse(**add_urls_to_folder(subfolder, item_count, auto_cover_url))
        entries.append(CategoryOverviewFolder(
            folder=folder_response, item_count=item_count, cover_url=folder_response.thumbnail_url,
        ))
    return entries


async def _resolve_midia_subtype_folder_ids(db: AsyncSession, user_id: int) -> List[int]:
    """Direct children of the user's Mídia-tagged folder(s) — i.e. Filmes/
    Séries/Animes/Hot — the same "pool these together" set
    build_media_folder_overview (routers/folders.py) expects. Mirrors how
    the web app's Mídia landing page resolves this (content_types.py)."""
    midia_folder_ids = await _resolve_midia_folder_ids(db, user_id)
    if not midia_folder_ids:
        return []

    subtype_folders = await db.execute(
        select(Folder.id).where(Folder.user_id == user_id, Folder.parent_id.in_(midia_folder_ids))
    )
    return list(subtype_folders.scalars().all())


@router.get("/browse")
async def tv_browse(
    type_id: Optional[int] = Query(
        None,
        description="Scope Destaques/Recentes to a single Mídia subtype folder "
        "(Séries/Filmes/Animes/Hot/...) — one of subtype_folders' ids. Omit to "
        "pool every subtype together (the default landing view).",
    ),
    genre: Optional[str] = Query(None, description="Filter Destaques/Recentes by genre."),
    sort: str = Query("recent", description="recent | oldest | name_asc | name_desc"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Get TV home screen data in a single request: continue watching (files),
    the Mídia subtype folders themselves (Séries/Filmes/Animes/Hot — for the
    category pill menu), featured/recent titles (Mídia folders —
    movies/shows, same source as the web app's Mídia landing page, optionally
    scoped to a single subtype and filtered by genre/sort), and the legacy
    flat folder list (kept for backward compat, the TV client no longer
    renders it). Optimized for TV client to minimize API calls.
    """
    from .content_types import _recursive_folder_ids
    from .folders import build_media_folder_overview

    # The subtype folders themselves (Séries/Filmes/Animes/Hot) — the
    # category pill menu ("Menu 1"). Always the full unfiltered set,
    # regardless of which one is currently selected, so a retagged subtype
    # (e.g. "xHot" edited to carry the Hot type) is still reachable there.
    midia_folder_ids = await _resolve_midia_folder_ids(db, current_user.id)
    subtype_folders = await _build_subtype_pills(db, midia_folder_ids)

    # Subtypes retagged to a different top-level type stay out of the
    # pooled "Visão geral" view and Continue Watching by default — only
    # showing up once their own Menu 1 pill is selected.
    hidden_subtype_ids = await _resolve_hidden_subtype_ids(db, current_user.id, midia_folder_ids)
    hidden_tree_ids = set(await _recursive_folder_ids(db, hidden_subtype_ids)) if hidden_subtype_ids else set()

    # Get continue watching, filtering out anything inside a hidden subtype
    continue_watching = await fetch_continue_watching_files(db, current_user.id, 20)
    if hidden_tree_ids:
        continue_watching = [f for f in continue_watching if f.folder_id not in hidden_tree_ids]

    # Get recent files
    recent_files = await fetch_recent_files(db, current_user.id, 20)

    # Get top-level folders (legacy — unused by the current TV home screen)
    folders_query = (
        select(Folder)
        .where(Folder.user_id == current_user.id, Folder.parent_id == None)
        .order_by(Folder.name)
    )
    folders_result = await db.execute(folders_query)
    folders = folders_result.scalars().all()

    # Mídia titles (movie/show folders) for the Destaques/Recentes rows —
    # pooled across every non-hidden subtype by default, or scoped to a
    # single one (type_id) when a category pill is selected — selecting a
    # hidden subtype's own pill still works, that's the "only appears if
    # you click Menu 1" escape hatch. genre/sort passed through the same
    # way the web app's Mídia page does.
    subtype_ids = await _resolve_midia_subtype_folder_ids(db, current_user.id)
    if type_id is not None and type_id in subtype_ids:
        scope_ids = [type_id]
    else:
        scope_ids = [sid for sid in subtype_ids if sid not in hidden_subtype_ids]

    featured_folders = []
    recent_folders = []
    if scope_ids:
        media_overview = await build_media_folder_overview(
            db, current_user.id, scope_ids, recent_limit=20, featured_limit=20, sort=sort, genre=genre,
        )
        featured_folders = media_overview.featured
        recent_folders = media_overview.recent

    return {
        "continue_watching": [add_urls_to_file(f) for f in continue_watching],
        "recent": [add_urls_to_file(f) for f in recent_files],
        "subtype_folders": subtype_folders,
        "featured_folders": featured_folders,
        "recent_folders": recent_folders,
        "folders": [
            {
                "id": f.id,
                "name": f.name,
                "parent_id": f.parent_id,
                "file_count": None  # Can be computed if needed
            }
            for f in folders
        ]
    }


@router.get("/continue")
async def tv_continue_watching(
    limit: int = Query(20, ge=1, le=50),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get continue watching list for TV."""
    files = await fetch_continue_watching_files(db, current_user.id, limit)
    return [add_urls_to_file(f) for f in files]


@router.get("/recent")
async def tv_recent_files(
    limit: int = Query(20, ge=1, le=50),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get recently added files for TV."""
    files = await fetch_recent_files(db, current_user.id, limit)
    return [add_urls_to_file(f) for f in files]


@router.get("/search")
async def tv_search(
    q: str = Query(..., min_length=1),
    limit: int = Query(30, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Search for the TV client — scoped to Mídia only, matching the
    folder-first (title) browsing the TV home screen uses. Files are
    scoped to the ones physically inside the Mídia folder tree (files don't
    reliably carry their own content_type_id — see resolve_category_
    breadcrumb); folder results are titles (movie/show folders), matched by
    name/title/genre, with the same cover/count enrichment as Destaques/
    Recentes so the search result cards look identical to them. Subtypes
    retagged to a different top-level type (see _resolve_hidden_subtype_ids)
    are left out — search has no per-category scoping on TV, so there's no
    "click Menu 1" escape hatch here; they just stay hidden."""
    from .content_types import _recursive_folder_ids
    from .folders import build_media_folder_overview

    midia_folder_ids = await _resolve_midia_folder_ids(db, current_user.id)
    hidden_subtype_ids = set(await _resolve_hidden_subtype_ids(db, current_user.id, midia_folder_ids))

    all_subtype_ids = await _resolve_midia_subtype_folder_ids(db, current_user.id)
    subtype_ids = [sid for sid in all_subtype_ids if sid not in hidden_subtype_ids]
    if not subtype_ids:
        return {"files": [], "folders": []}

    midia_tree_ids = await _recursive_folder_ids(db, subtype_ids)

    # Search files by name, scoped to the Mídia subtree
    files_query = (
        select(File)
        .where(
            File.user_id == current_user.id,
            File.folder_id.in_(midia_tree_ids),
            File.file_name.ilike(f"%{escape_like(q)}%", escape="\\")
        )
        .options(selectinload(File.watch_progress))
        .order_by(desc(File.created_at))
        .limit(limit)
    )
    files_result = await db.execute(files_query)
    files = files_result.scalars().all()

    # Search titles (movie/show folders) by name/title/genre — reuses the
    # same per-title cover/item_count resolution as Destaques/Recentes so
    # a search result card matches them exactly.
    overview = await build_media_folder_overview(
        db, current_user.id, subtype_ids, recent_limit=0, featured_limit=0, sort="name",
    )
    needle = q.strip().lower()
    matched_folders = [
        entry for entry in overview.subfolders
        if needle in entry.folder.name.lower()
        or (entry.folder.title and needle in entry.folder.title.lower())
        or any(needle in g.lower() for g in entry.folder.genres)
    ][:limit]

    return {
        "files": [add_urls_to_file(f) for f in files],
        "folders": matched_folders,
    }


@router.get("/folder/{folder_id}")
async def tv_folder_detail(
    folder_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Get folder details with files and subfolders for TV client.
    Returns folder info, subfolders, files, and parent path for navigation.
    """
    from fastapi import HTTPException
    
    # Get the folder
    folder_result = await db.execute(
        select(Folder).where(Folder.id == folder_id, Folder.user_id == current_user.id)
    )
    folder = folder_result.scalar_one_or_none()
    
    if not folder:
        raise HTTPException(status_code=404, detail="Folder not found")
    
    # Get subfolders
    subfolders_result = await db.execute(
        select(Folder)
        .where(Folder.user_id == current_user.id, Folder.parent_id == folder_id)
        .order_by(Folder.name)
    )
    subfolders = subfolders_result.scalars().all()
    
    # Get files in this folder
    files_result = await db.execute(
        select(File)
        .where(File.user_id == current_user.id, File.folder_id == folder_id)
        .options(selectinload(File.watch_progress))
        .order_by(File.file_name)
    )
    files = files_result.scalars().all()
    
    # Build parent path for breadcrumb navigation
    parent_path = []
    current_folder = folder
    while current_folder.parent_id:
        parent_result = await db.execute(
            select(Folder).where(Folder.id == current_folder.parent_id)
        )
        parent = parent_result.scalar_one_or_none()
        if parent:
            parent_path.insert(0, {
                "id": parent.id,
                "name": parent.name,
                "parent_id": parent.parent_id,
                "user_id": parent.user_id,
                "created_at": parent.created_at.isoformat() if parent.created_at else None,
                "updated_at": parent.updated_at.isoformat() if parent.updated_at else None,
            })
            current_folder = parent
        else:
            break
    
    return {
        "folder": {
            "id": folder.id,
            "name": folder.name,
            "parent_id": folder.parent_id,
            "user_id": folder.user_id,
            "created_at": folder.created_at.isoformat() if folder.created_at else None,
            "updated_at": folder.updated_at.isoformat() if folder.updated_at else None,
        },
        "subfolders": [
            {
                "id": sf.id,
                "name": sf.name,
                "parent_id": sf.parent_id,
                "user_id": sf.user_id,
                "created_at": sf.created_at.isoformat() if sf.created_at else None,
                "updated_at": sf.updated_at.isoformat() if sf.updated_at else None,
            }
            for sf in subfolders
        ],
        "files": [add_urls_to_file(f) for f in files],
        "parent_path": parent_path
    }
