/**
 * Named cover crop formats. Add a new entry here (and a way to pick it in
 * ChangeCoverModal) when a second format is needed — everything else in the
 * crop flow already reads from this table instead of a hardcoded ratio.
 */
export const COVER_ASPECT_RATIOS = {
    card: 16 / 9, // matches the FileCard grid thumbnail (Tailwind's aspect-video)
    // portrait: 320 / 404, // planned: a second cover format for another use
} as const;

export type CoverAspectRatioKey = keyof typeof COVER_ASPECT_RATIOS;
