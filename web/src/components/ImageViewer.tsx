/**
 * ImageViewer - full screen lightbox for viewing images, with zoom, pan,
 * and next/previous navigation within the current folder.
 */
import { useState, useEffect, useRef, useCallback } from 'react';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, RotateCcw, AlertTriangle, Download } from 'lucide-react';
import { TelegramFile } from '../lib/api';

interface ImageViewerProps {
    file: TelegramFile;
    images: TelegramFile[];
    onClose: () => void;
    onNavigate: (file: TelegramFile) => void;
}

const MIN_ZOOM = 1;
const MAX_ZOOM = 6;

export default function ImageViewer({ file, images, onClose, onNavigate }: ImageViewerProps) {
    const [zoom, setZoom] = useState(1);
    const [pan, setPan] = useState({ x: 0, y: 0 });
    const [isLoading, setIsLoading] = useState(true);
    const [hasError, setHasError] = useState(false);
    const [isDragging, setIsDragging] = useState(false);

    const dragStart = useRef({ x: 0, y: 0 });
    const panStart = useRef({ x: 0, y: 0 });
    const pinchStartDistance = useRef<number | null>(null);
    const pinchStartZoom = useRef(1);
    const containerRef = useRef<HTMLDivElement>(null);

    const currentIndex = images.findIndex((f) => f.id === file.id);
    const hasMultiple = images.length > 1;
    const canGoPrev = hasMultiple && currentIndex > 0;
    const canGoNext = hasMultiple && currentIndex >= 0 && currentIndex < images.length - 1;

    // Reset zoom/pan and loading state whenever the displayed image changes
    useEffect(() => {
        setZoom(1);
        setPan({ x: 0, y: 0 });
        setIsLoading(true);
        setHasError(false);
    }, [file.id]);

    const goToPrev = useCallback(() => {
        if (canGoPrev) onNavigate(images[currentIndex - 1]);
    }, [canGoPrev, images, currentIndex, onNavigate]);

    const goToNext = useCallback(() => {
        if (canGoNext) onNavigate(images[currentIndex + 1]);
    }, [canGoNext, images, currentIndex, onNavigate]);

    const zoomIn = useCallback(() => {
        setZoom((z) => Math.min(MAX_ZOOM, +(z + 0.5).toFixed(2)));
    }, []);

    const zoomOut = useCallback(() => {
        setZoom((z) => {
            const next = Math.max(MIN_ZOOM, +(z - 0.5).toFixed(2));
            if (next === MIN_ZOOM) setPan({ x: 0, y: 0 });
            return next;
        });
    }, []);

    const resetZoom = useCallback(() => {
        setZoom(1);
        setPan({ x: 0, y: 0 });
    }, []);

    // Keyboard shortcuts
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            switch (e.key) {
                case 'Escape':
                    onClose();
                    break;
                case 'ArrowLeft':
                    goToPrev();
                    break;
                case 'ArrowRight':
                    goToNext();
                    break;
                case '+':
                case '=':
                    zoomIn();
                    break;
                case '-':
                    zoomOut();
                    break;
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [onClose, goToPrev, goToNext, zoomIn, zoomOut]);

    // Mouse wheel zoom (centered roughly on cursor via simple scale, no offset math needed for a lightbox)
    const handleWheel = (e: React.WheelEvent) => {
        e.preventDefault();
        if (e.deltaY < 0) {
            setZoom((z) => Math.min(MAX_ZOOM, +(z + 0.25).toFixed(2)));
        } else {
            setZoom((z) => {
                const next = Math.max(MIN_ZOOM, +(z - 0.25).toFixed(2));
                if (next === MIN_ZOOM) setPan({ x: 0, y: 0 });
                return next;
            });
        }
    };

    // Mouse drag to pan when zoomed in
    const handleMouseDown = (e: React.MouseEvent) => {
        if (zoom <= 1) return;
        e.preventDefault();
        setIsDragging(true);
        dragStart.current = { x: e.clientX, y: e.clientY };
        panStart.current = { ...pan };
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (!isDragging) return;
        const dx = e.clientX - dragStart.current.x;
        const dy = e.clientY - dragStart.current.y;
        setPan({ x: panStart.current.x + dx, y: panStart.current.y + dy });
    };

    const stopDragging = () => setIsDragging(false);

    // Touch: single-finger pan when zoomed, two-finger pinch to zoom
    const touchDistance = (touches: React.TouchList) => {
        const [a, b] = [touches[0], touches[1]];
        return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
    };

    const handleTouchStart = (e: React.TouchEvent) => {
        if (e.touches.length === 2) {
            pinchStartDistance.current = touchDistance(e.touches);
            pinchStartZoom.current = zoom;
        } else if (e.touches.length === 1 && zoom > 1) {
            setIsDragging(true);
            dragStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
            panStart.current = { ...pan };
        }
    };

    const handleTouchMove = (e: React.TouchEvent) => {
        if (e.touches.length === 2 && pinchStartDistance.current) {
            e.preventDefault();
            const newDistance = touchDistance(e.touches);
            const scale = newDistance / pinchStartDistance.current;
            const next = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(pinchStartZoom.current * scale).toFixed(2)));
            setZoom(next);
            if (next === MIN_ZOOM) setPan({ x: 0, y: 0 });
        } else if (e.touches.length === 1 && isDragging) {
            const dx = e.touches[0].clientX - dragStart.current.x;
            const dy = e.touches[0].clientY - dragStart.current.y;
            setPan({ x: panStart.current.x + dx, y: panStart.current.y + dy });
        }
    };

    const handleTouchEnd = (e: React.TouchEvent) => {
        if (e.touches.length < 2) pinchStartDistance.current = null;
        if (e.touches.length === 0) setIsDragging(false);
    };

    const token = localStorage.getItem('access_token');
    const authorizedUrl = `${file.stream_url}?token=${token}`;

    return (
        <div
            className="fixed inset-0 bg-black/95 z-[100] flex items-center justify-center select-none"
            onClick={(e) => {
                // Close only when clicking the backdrop itself, not the image or controls
                if (e.target === e.currentTarget) onClose();
            }}
        >
            {/* Top bar */}
            <div className="absolute top-0 left-0 right-0 p-4 bg-gradient-to-b from-black/80 to-transparent flex items-start justify-between z-10">
                <div className="min-w-0">
                    <h3 className="text-sm font-medium truncate max-w-md text-white">{file.file_name}</h3>
                    {hasMultiple && (
                        <p className="text-xs text-dark-400">
                            {currentIndex + 1} de {images.length}
                        </p>
                    )}
                </div>
                <button
                    onClick={onClose}
                    className="p-2 text-white hover:bg-white/20 rounded-full transition-colors shrink-0"
                >
                    <X className="w-6 h-6" />
                </button>
            </div>

            {/* Previous / Next arrows */}
            {hasMultiple && canGoPrev && (
                <button
                    onClick={(e) => { e.stopPropagation(); goToPrev(); }}
                    className="absolute left-2 sm:left-6 top-1/2 -translate-y-1/2 p-3 text-white/70 hover:text-white hover:bg-white/10 rounded-full transition-all z-10"
                    title="Foto anterior"
                >
                    <ChevronLeft className="w-8 h-8" />
                </button>
            )}
            {hasMultiple && canGoNext && (
                <button
                    onClick={(e) => { e.stopPropagation(); goToNext(); }}
                    className="absolute right-2 sm:right-6 top-1/2 -translate-y-1/2 p-3 text-white/70 hover:text-white hover:bg-white/10 rounded-full transition-all z-10"
                    title="Próxima foto"
                >
                    <ChevronRight className="w-8 h-8" />
                </button>
            )}

            {/* Image area */}
            <div
                ref={containerRef}
                className="w-full h-full flex items-center justify-center overflow-hidden"
                onWheel={handleWheel}
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={stopDragging}
                onMouseLeave={stopDragging}
                onTouchStart={handleTouchStart}
                onTouchMove={handleTouchMove}
                onTouchEnd={handleTouchEnd}
            >
                {hasError ? (
                    <div className="text-center p-8 max-w-md glass-panel animate-scale-in" onClick={(e) => e.stopPropagation()}>
                        <div className="w-16 h-16 rounded-2xl bg-yellow-500/20 flex items-center justify-center mx-auto mb-5 border border-yellow-500/30">
                            <AlertTriangle className="w-8 h-8 text-yellow-400" />
                        </div>
                        <h3 className="text-xl font-bold text-white mb-2">Não foi possível carregar a imagem</h3>
                        <p className="text-dark-300 mb-6">O arquivo pode estar corrompido ou indisponível.</p>
                        <a
                            href={authorizedUrl + '&download=1'}
                            className="btn-secondary inline-flex items-center justify-center gap-2"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <Download className="w-4 h-4" />
                            Baixar mesmo assim
                        </a>
                    </div>
                ) : (
                    <>
                        {isLoading && (
                            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                                <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary-500"></div>
                            </div>
                        )}
                        <img
                            src={authorizedUrl}
                            alt={file.file_name}
                            draggable={false}
                            onClick={(e) => e.stopPropagation()}
                            onLoad={() => setIsLoading(false)}
                            onError={() => { setIsLoading(false); setHasError(true); }}
                            className={`max-w-full max-h-full object-contain transition-opacity duration-200 ${isLoading ? 'opacity-0' : 'opacity-100'} ${zoom > 1 ? (isDragging ? 'cursor-grabbing' : 'cursor-grab') : 'cursor-zoom-in'}`}
                            style={{
                                transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
                                transition: isDragging ? 'none' : 'transform 0.15s ease-out',
                            }}
                            onDoubleClick={(e) => {
                                e.stopPropagation();
                                if (zoom > 1) resetZoom();
                                else setZoom(2);
                            }}
                        />
                    </>
                )}
            </div>

            {/* Bottom zoom controls */}
            {!hasError && (
                <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-2 bg-dark-900/80 backdrop-blur-md rounded-full px-3 py-2 border border-white/10 z-10">
                    <button
                        onClick={(e) => { e.stopPropagation(); zoomOut(); }}
                        disabled={zoom <= MIN_ZOOM}
                        className="p-2 rounded-full text-white/80 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        title="Diminuir zoom"
                    >
                        <ZoomOut className="w-4 h-4" />
                    </button>
                    <span className="text-xs text-white/70 font-mono w-12 text-center">{Math.round(zoom * 100)}%</span>
                    <button
                        onClick={(e) => { e.stopPropagation(); zoomIn(); }}
                        disabled={zoom >= MAX_ZOOM}
                        className="p-2 rounded-full text-white/80 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        title="Aumentar zoom"
                    >
                        <ZoomIn className="w-4 h-4" />
                    </button>
                    {zoom > 1 && (
                        <button
                            onClick={(e) => { e.stopPropagation(); resetZoom(); }}
                            className="p-2 rounded-full text-white/80 hover:text-white hover:bg-white/10 transition-colors"
                            title="Redefinir zoom"
                        >
                            <RotateCcw className="w-4 h-4" />
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}
