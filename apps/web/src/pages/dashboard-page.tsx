import { useMe } from "@/auth/use-me";

export function DashboardPage() {
  const { data, isLoading, isError, error } = useMe();

  return (
    <div className="space-y-2">
      <h1 className="text-xl font-semibold">Ana Sayfa</h1>
      {isLoading && <p className="text-muted-foreground text-sm">Profil yükleniyor…</p>}
      {isError && (
        <p className="text-destructive text-sm">
          Profil alınamadı: {error instanceof Error ? error.message : "bilinmeyen hata"}
        </p>
      )}
      {data && (
        <p className="text-sm">
          Hoş geldin, <strong>{data.name}</strong> ({data.role})
        </p>
      )}
    </div>
  );
}
