import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { getUsageReport, downloadUsageReportXlsx } from "@/api/report";

// C-11 (#85): GET /admin/reports/usage + xlsx export - yalnizca system_admin.
export function AdminReportsPage() {
  const { token } = useAuth();
  const [downloading, setDownloading] = useState(false);

  const { data: report, isLoading, isError } = useQuery({
    queryKey: ["reports", "usage"],
    queryFn: () => getUsageReport(token!),
    enabled: Boolean(token),
  });

  async function handleExport() {
    if (!token) return;
    setDownloading(true);
    try {
      const blob = await downloadUsageReportXlsx(token);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "usage-report.xlsx";
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Rapor indirilemedi.");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Kullanım Raporu</h1>
          <p className="text-muted-foreground text-sm">
            Modül bazlı kullanım özeti. Yalnızca system_admin erişebilir.
          </p>
        </div>
        <Button onClick={handleExport} disabled={downloading || !report}>
          <Download />
          {downloading ? "İndiriliyor…" : "Excel'e Aktar"}
        </Button>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Rapor yüklenemedi.</p>}

      {report && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Modül</th>
                <th className="px-4 py-2 text-left font-medium">Metrik</th>
                <th className="px-4 py-2 text-right font-medium">Sayı</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {report.rows.map((row, index) => (
                <tr key={index}>
                  <td className="px-4 py-2 font-medium">{row.module}</td>
                  <td className="text-muted-foreground px-4 py-2">{row.metric}</td>
                  <td className="px-4 py-2 text-right">{row.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
