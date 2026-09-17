/**
 * ChangeCoverModal - lets the user upload a local image, crop it to the
 * card's thumbnail ratio, and save it as the file's custom cover.
 */
import { useState, useCallback } from 'react';
import { X, Upload, ZoomIn, ZoomOut } from 'lucide-react';
import Cropper, { Area } from 'react-easy-crop';
import { TelegramFile, useUploadThumbnail } from '../lib/api';
import { useAppStore } from '../lib/store';
import { getCroppedImageBlob } from '../lib/cropImage';
import { COVER_ASPECT_RATIOS } from '../lib/coverAspectRatios';

interface ChangeCoverModalProps {
    file: TelegramFile;
    onClose: () => void;
    onChanged: (updated: TelegramFile) => void;
}

export default function ChangeCoverModal({ file, onClose, onChanged }: ChangeCoverModalProps) {
    const [imageSrc, setImageSrc] = useState<string | null>(null);
    const [crop, setCrop] = useState({ x: 0, y: 0 });
    const [zoom, setZoom] = useState(1);
    const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);
    const { addToast } = useAppStore();
    const uploadThumbnail = useUploadThumbnail();

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files?.[0];
        if (!selected) return;
        if (!selected.type.startsWith('image/')) {
            addToast('Selecione um arquivo de imagem', 'error');
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            setImageSrc(reader.result as string);
            setCrop({ x: 0, y: 0 });
            setZoom(1);
            setCroppedAreaPixels(null);
        };
        reader.readAsDataURL(selected);
    };

    const onCropComplete = useCallback((_: Area, areaPixels: Area) => {
        setCroppedAreaPixels(areaPixels);
    }, []);

    const handleConfirm = async () => {
        if (!imageSrc || !croppedAreaPixels) return;
        try {
            const blob = await getCroppedImageBlob(imageSrc, croppedAreaPixels);
            const updated = await uploadThumbnail.mutateAsync({ id: file.id, blob });
            onChanged(updated);
            addToast('Capa atualizada com sucesso');
            onClose();
        } catch (error) {
            addToast('Falha ao atualizar a capa', 'error');
        }
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
            <div className="glass-card w-full max-w-lg p-6 animate-scale-in">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold truncate pr-4">Alterar capa</h2>
                    <button onClick={onClose} className="p-1 hover:bg-dark-700 rounded shrink-0">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {!imageSrc ? (
                    <label className="flex flex-col items-center justify-center gap-3 border-2 border-dashed border-white/10 rounded-xl p-10 cursor-pointer hover:border-primary-500/40 hover:bg-white/[0.02] transition-colors">
                        <Upload className="w-8 h-8 text-dark-400" />
                        <span className="text-sm text-dark-300">Clique para escolher uma imagem</span>
                        <input type="file" accept="image/*" className="hidden" onChange={handleFileSelect} />
                    </label>
                ) : (
                    <>
                        <div className="relative w-full h-72 bg-dark-950 rounded-lg overflow-hidden">
                            <Cropper
                                image={imageSrc}
                                crop={crop}
                                zoom={zoom}
                                aspect={COVER_ASPECT_RATIOS.card}
                                onCropChange={setCrop}
                                onZoomChange={setZoom}
                                onCropComplete={onCropComplete}
                            />
                        </div>

                        <div className="flex items-center gap-3 mt-4">
                            <ZoomOut className="w-4 h-4 text-dark-400 shrink-0" />
                            <input
                                type="range"
                                min={1}
                                max={3}
                                step={0.01}
                                value={zoom}
                                onChange={(e) => setZoom(Number(e.target.value))}
                                className="w-full accent-primary-500"
                            />
                            <ZoomIn className="w-4 h-4 text-dark-400 shrink-0" />
                        </div>

                        <button
                            onClick={() => setImageSrc(null)}
                            className="text-xs text-dark-400 hover:text-white mt-3 transition-colors"
                        >
                            Escolher outra imagem
                        </button>
                    </>
                )}

                <div className="flex justify-end gap-3 mt-6">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-dark-400 hover:text-white transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        onClick={handleConfirm}
                        disabled={!imageSrc || !croppedAreaPixels || uploadThumbnail.isPending}
                        className="px-4 py-2 bg-primary-600 hover:bg-primary-700 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                        {uploadThumbnail.isPending ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Salvando...
                            </>
                        ) : (
                            'Salvar Capa'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}
