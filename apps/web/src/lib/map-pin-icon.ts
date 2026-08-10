import { divIcon } from "leaflet";

// leaflet'in varsayilan marker ikonu ek asset (png) yapilandirmasi gerektirir;
// bunun yerine self-contained bir SVG pin kullanilir. B-27'de arama sonucu
// icin, B-30'da admin durak konumu secici icin kullanilir.
export function createPinIcon(fillColor: string, strokeColor: string) {
  return divIcon({
    className: "",
    html: `<svg width="28" height="40" viewBox="0 0 28 40" xmlns="http://www.w3.org/2000/svg">
      <path d="M14 0C6.3 0 0 6.3 0 14c0 10.5 14 26 14 26s14-15.5 14-26c0-7.7-6.3-14-14-14z" fill="${fillColor}" stroke="${strokeColor}" stroke-width="1.5" />
      <circle cx="14" cy="14" r="5" fill="#fff" />
    </svg>`,
    iconSize: [28, 40],
    iconAnchor: [14, 40],
  });
}
