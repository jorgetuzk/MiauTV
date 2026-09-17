/**
 * MoveFileModal - modal for selecting a destination folder
 */
import { useState, useEffect, useMemo } from 'react';
import { X, Folder as FolderIcon, ChevronRight, Home, Search } from 'lucide-react';
import { useFolderTree, TelegramFile, Folder, useMoveFiles, useMoveFolders } from '../lib/api';
import { useAppStore } from '../lib/store';

interface MoveFileModalProps {
    items: { files: TelegramFile[]; folders: Folder[] };
    onClose: () => void;
}

interface FlatFolder {
    id: number;
    name: string;
    depth: number;
    file_count: number;
}

function flattenTree(folders: Folder[], depth = 0): FlatFolder[] {
    const result: FlatFolder[] = [];
    for (const folder of folders) {
        result.push({ id: folder.id, name: folder.name, depth, file_count: folder.file_count });
        if (folder.children?.length) {
            result.push(...flattenTree(folder.children, depth + 1));
        }
    }
    return result;
}

export default function MoveFileModal({ items, onClose }: MoveFileModalProps) {
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [searchInput, setSearchInput] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const { data: folderTree, isLoading } = useFolderTree();
    const { mutateAsync: moveFiles, isPending: isFilesPending } = useMoveFiles();
    const { mutateAsync: moveFolders, isPending: isFoldersPending } = useMoveFolders();
    const { addToast, clearSelection } = useAppStore();

    const isPending = isFilesPending || isFoldersPending;
    const totalItems = items.files.length + items.folders.length;

    // Debounce the search input a little so we don't re-filter on every keystroke.
    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(searchInput.trim().toLowerCase()), 180);
        return () => clearTimeout(timer);
    }, [searchInput]);

    const flatFolders = useMemo(() => flattenTree(folderTree || []), [folderTree]);

    const searchResults = useMemo(() => {
        if (!debouncedSearch) return null;
        return flatFolders.filter((f) => f.name.toLowerCase().includes(debouncedSearch));
    }, [flatFolders, debouncedSearch]);

    const handleMove = async () => {
        try {
            const promises = [];
            if (items.files.length > 0) {
                promises.push(moveFiles({ ids: items.files.map(f => f.id), folderId: selectedId }));
            }
            if (items.folders.length > 0) {
                // Prevent moving folder into itself
                const folderIds = items.folders.map(f => f.id);
                if (selectedId && folderIds.includes(selectedId)) {
                    addToast('Não é possível mover uma pasta para dentro dela mesma', 'error');
                    return;
                }
                promises.push(moveFolders({ ids: folderIds, folderId: selectedId }));
            }

            await Promise.all(promises);
            addToast(`${totalItems} item(ns) movido(s) com sucesso`);
            clearSelection();
            onClose();
        } catch (error) {
            addToast('Falha ao mover os itens', 'error');
        }
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
            <div className="glass-card w-full max-w-md p-6 animate-scale-in">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold">Mover {totalItems} Ite{totalItems !== 1 ? 'ns' : 'm'}</h2>
                    <button onClick={onClose} className="p-1 hover:bg-dark-700 rounded">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <p className="text-sm text-dark-400 mb-3 truncate">
                    Selecione a pasta de destino
                </p>

                <div className="relative mb-3">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-500" />
                    <input
                        type="text"
                        value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        placeholder="Buscar pasta..."
                        className="w-full bg-dark-800 border border-white/[0.06] rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:border-primary-500/50 focus:bg-dark-700 transition-all"
                    />
                </div>

                <div className="bg-dark-800 rounded-lg max-h-64 overflow-y-auto mb-4 custom-scrollbar">
                    {searchResults ? (
                        searchResults.length > 0 ? (
                            searchResults.map((folder) => (
                                <button
                                    key={folder.id}
                                    onClick={() => setSelectedId(folder.id)}
                                    className={`w-full flex items-center gap-2 px-4 py-2 hover:bg-dark-700 transition-colors ${selectedId === folder.id ? 'bg-primary-600/20 text-primary-400' : ''
                                        }`}
                                >
                                    <FolderIcon className="w-4 h-4 text-primary-400 shrink-0" />
                                    <span className="truncate">{folder.name}</span>
                                    <span className="text-xs text-dark-500 ml-auto">{folder.file_count}</span>
                                </button>
                            ))
                        ) : (
                            <div className="p-4 text-center text-dark-400 text-sm">Nenhuma pasta encontrada</div>
                        )
                    ) : (
                        <>
                            {/* Root option */}
                            <button
                                onClick={() => setSelectedId(null)}
                                className={`w-full flex items-center gap-2 px-4 py-3 hover:bg-dark-700 transition-colors ${selectedId === null ? 'bg-primary-600/20 text-primary-400' : ''
                                    }`}
                            >
                                <Home className="w-4 h-4" />
                                <span>Raiz (Sem pasta)</span>
                            </button>

                            {isLoading ? (
                                <div className="p-4 text-center text-dark-400">Carregando...</div>
                            ) : (
                                folderTree?.map((folder) => (
                                    <FolderTreeItem
                                        key={folder.id}
                                        folder={folder}
                                        selectedId={selectedId}
                                        onSelect={setSelectedId}
                                        depth={0}
                                    />
                                ))
                            )}
                        </>
                    )}
                </div>

                <div className="flex justify-end gap-3">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-dark-400 hover:text-white transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        onClick={handleMove}
                        disabled={isPending}
                        className="px-4 py-2 bg-primary-600 hover:bg-primary-700 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                        {isPending ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Movendo...
                            </>
                        ) : (
                            'Mover Para Cá'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

function FolderTreeItem({ folder, selectedId, onSelect, depth }: {
    folder: Folder;
    selectedId: number | null;
    onSelect: (id: number) => void;
    depth: number;
}) {
    const [expanded, setExpanded] = useState(true);
    const hasChildren = folder.children && folder.children.length > 0;

    return (
        <div>
            <button
                onClick={() => onSelect(folder.id)}
                className={`w-full flex items-center gap-2 px-4 py-2 hover:bg-dark-700 transition-colors ${selectedId === folder.id ? 'bg-primary-600/20 text-primary-400' : ''
                    }`}
                style={{ paddingLeft: `${16 + depth * 16}px` }}
            >
                {hasChildren && (
                    <div
                        onClick={(e) => { e.stopPropagation(); setExpanded(!expanded); }}
                        className="p-0.5"
                    >
                        <ChevronRight className={`w-3 h-3 transition-transform ${expanded ? 'rotate-90' : ''}`} />
                    </div>
                )}
                {!hasChildren && <div className="w-4" />}
                <FolderIcon className="w-4 h-4 text-primary-400" />
                <span className="truncate">{folder.name}</span>
                <span className="text-xs text-dark-500 ml-auto">{folder.file_count}</span>
            </button>

            {expanded && hasChildren && folder.children?.map((child) => (
                <FolderTreeItem
                    key={child.id}
                    folder={child}
                    selectedId={selectedId}
                    onSelect={onSelect}
                    depth={depth + 1}
                />
            ))}
        </div>
    );
}
