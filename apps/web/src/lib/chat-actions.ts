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
  shuttle_routes: "/shuttle/routes",
  shuttle_recommendation: "/shuttle/recommendation",
  directory_employees: "/directory/employees",
  directory_departments: "/directory/departments",
  vehicles: "/vehicles",
  dashboard: "/",
};

export function routeForAction(target: string): string | null {
  return ACTION_ROUTES[target] ?? null;
}
