"""
TV-specific API endpoints optimized for Android TV clients.
"""
from typing import List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc
from sqlalchemy.orm import selectinload

from ..database import get_db
from ..models import File, User, Folder, WatchProgress, ContentType
from ..auth import get_current_user
from ..config import get_settings
from ..services import (
    escape_like,
    add_urls_to_file,
    fetch_recent_files,
    fetch_continue_watching_files
)

router = APIRouter(prefix="/tv", tags=["TV"])
settings = get_settings()


async def _resolve_midia_subtype_folder_ids(db: AsyncSession, user_id: int) -> List[int]:
    """Direct children of the user's Mídia-tagged folder(s) — i.e. Filmes/
    Séries/Animes — the same "pool these together" set
    build_media_folder_overview (routers/folders.py) expects. Mirrors how
    the web app's Mídia landing page resolves this (content_types.py)."""
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
        select(Folder.id).where(Folder.user_id == user_id, Folder.content_type_id == midia_type_id)
    )
    midia_folder_ids = list(midia_folders.scalars().all())
    if not midia_folder_ids:
        return []

    subtype_folders = await db.execute(
        select(Folder.id).where(Folder.user_id == user_id, Folder.parent_id.in_(midia_folder_ids))
    )
    return list(subtype_folders.scalars().all())


@router.get("/browse")
async def tv_browse(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Get TV home screen data in a single request: continue watching (files),
    featured/recent titles (Mídia folders — movies/shows, same source as the
    web app's Mídia landing page), and the legacy flat folder list (kept for
    backward compat, the TV client no longer renders it).
    Optimized for TV client to minimize API calls.
    """
    from .folders import build_media_folder_overview

    # Get continue watching
    continue_watching = await fetch_continue_watching_files(db, current_user.id, 20)

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

    # Mídia titles (movie/show folders) for the Destaques/Recentes rows
    subtype_ids = await _resolve_midia_subtype_folder_ids(db, current_user.id)
    media_overview = await build_media_folder_overview(
        db, current_user.id, subtype_ids, recent_limit=20, featured_limit=20,
    )

    return {
        "continue_watching": [add_urls_to_file(f) for f in continue_watching],
        "recent": [add_urls_to_file(f) for f in recent_files],
        "featured_folders": media_overview.featured,
        "recent_folders": media_overview.recent,
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
    Recentes so the search result cards look identical to them."""
    from .content_types import _recursive_folder_ids
    from .folders import build_media_folder_overview

    subtype_ids = await _resolve_midia_subtype_folder_ids(db, current_user.id)
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
