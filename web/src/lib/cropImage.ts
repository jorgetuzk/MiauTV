/**
 * Canvas-based helper to turn a react-easy-crop pixel crop area into an
 * uploadable JPEG Blob. Standard pattern for this library — react-easy-crop
 * only reports crop geometry, it doesn't produce the final image itself.
 */
import type { Area } from 'react-easy-crop';

function loadImage(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = () => resolve(img);
        img.onerror = reject;
        img.src = url;
    });
}

export async function getCroppedImageBlob(
    imageSrc: string,
    cropAreaPixels: Area,
    outputWidth = 960,
    quality = 0.9
): Promise<Blob> {
    const image = await loadImage(imageSrc);

    const canvas = document.createElement('canvas');
    const outputHeight = Math.round(outputWidth * (cropAreaPixels.height / cropAreaPixels.width));
    canvas.width = outputWidth;
    canvas.height = outputHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas 2D context not available');

    ctx.drawImage(
        image,
        cropAreaPixels.x,
        cropAreaPixels.y,
        cropAreaPixels.width,
        cropAreaPixels.height,
        0,
        0,
        outputWidth,
        outputHeight
    );

    return new Promise((resolve, reject) => {
        canvas.toBlob(
            (blob) => (blob ? resolve(blob) : reject(new Error('Failed to encode image'))),
            'image/jpeg',
            quality
        );
    });
}
