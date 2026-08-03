/**
 * A-22: chatbot yanitindaki SEMANTIK hedefi uygulama rotasina cevirir.
 *
 * Backend web URL'i dondurmuyor ("directory_employees", "/directory/employees" degil),
 * cunku Faz 2'de mobil istemci ayni yaniti kullanacak. Rota bilgisi istemciye ait ve
 * haritasi burada duruyor.
 *
 * Bilinmeyen hedef null doner ve buton HIC gosterilmez: backend yeni bir hedef
 * eklediginde web guncellenene kadar kirik bir link gostermektense butonu gizlemek
 * dogru davranis. Sessiz kalir ama yanlis yere goturmez.
 */
const ACTION_ROUTES: Record<string, string> = {
  menu: "/menu",
  // B-13 iki servis sayfasini (guzergahlar + en yakin servis) tek /shuttle altinda
  // birlestirdi; A-22'de yazilan /shuttle/routes ve /shuttle/recommendation rotalari
  // artik YOK ve butonlar bos sayfaya goturuyordu. Iki hedef de ayni sayfaya cikiyor,
  // ancak SEMANTIK ayrim korunuyor: sayfa ileride tekrar bolunurse yalnizca bu harita
  // degisir, backend ve mobil istemci etkilenmez.
  shuttle_routes: "/shuttle",
  shuttle_recommendation: "/shuttle",
  directory_employees: "/directory/employees",
  directory_departments: "/directory/departments",
  vehicles: "/vehicles",
  dashboard: "/",
};

export function routeForAction(target: string): string | null {
  return ACTION_ROUTES[target] ?? null;
}
