import { useState } from "react";
import { useNavigate } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { changePassword } from "@/api/auth";
import { ApiError } from "@/api/client";
import { useAuth } from "@/auth/auth-context";
import { clearMustChangePassword } from "@/auth/token-store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const MIN_LENGTH = 8;

/**
 * A-29 (#178): sifre degistirme ekrani.
 *
 * <p>Iki durumda kullanilir: sistem tarafindan uretilen gecici sifreyle giris yapildiginda
 * (RequireAuth buraya yonlendirir) ve kullanici kendi istegiyle sifresini degistirmek
 * istediginde.
 *
 * <p>Zorunlu akista "vazgec" secenegi YOK — kullanicinin gecici sifreyle dolasmasi tam da
 * onlemek istedigimiz durum.
 */
export function ChangePasswordPage() {
  const navigate = useNavigate();
  const { token, mustChangePassword } = useAuth();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordAgain, setNewPasswordAgain] = useState("");

  const mutation = useMutation({
    mutationFn: () => changePassword({ currentPassword, newPassword }, token!),
    onSuccess: () => {
      clearMustChangePassword();
      toast.success("Şifren güncellendi.");
      navigate("/", { replace: true });
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Şifre değiştirilemedi.");
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (newPassword.length < MIN_LENGTH) {
      toast.error(`Yeni şifre en az ${MIN_LENGTH} karakter olmalıdır.`);
      return;
    }
    // Sunucu da kontrol ediyor; buradaki amac kullaniciyi gereksiz bir istekten kurtarmak.
    if (newPassword !== newPasswordAgain) {
      toast.error("Yeni şifreler eşleşmiyor.");
      return;
    }
    mutation.mutate();
  }

  return (
    <div className="bg-background flex min-h-screen items-center justify-center px-4">
      <Card className="w-full max-w-md">
        <CardContent className="space-y-5">
          <div className="space-y-1.5">
            <h1 className="text-xl font-semibold">Şifreni belirle</h1>
            <p className="text-muted-foreground text-sm">
              {mustChangePassword
                ? "Hesabın geçici bir şifreyle oluşturuldu. Devam etmek için kendi şifreni belirlemelisin."
                : "Mevcut şifreni doğrulayıp yeni bir şifre belirleyebilirsin."}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="current-password">Mevcut şifre</Label>
              <Input
                id="current-password"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="new-password">Yeni şifre</Label>
              <Input
                id="new-password"
                type="password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
              <p className="text-muted-foreground text-xs">En az {MIN_LENGTH} karakter.</p>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="new-password-again">Yeni şifre (tekrar)</Label>
              <Input
                id="new-password-again"
                type="password"
                autoComplete="new-password"
                value={newPasswordAgain}
                onChange={(event) => setNewPasswordAgain(event.target.value)}
              />
            </div>

            <div className="flex gap-2">
              <Button type="submit" className="flex-1" disabled={mutation.isPending}>
                {mutation.isPending ? "Kaydediliyor…" : "Şifreyi güncelle"}
              </Button>
              {/* Vazgecme secenegi YALNIZCA istege bagli akista. Gecici sifreyle giren
                  kullanicinin uygulamada dolasmasi tam da onlemek istedigimiz durum. */}
              {!mustChangePassword && (
                <Button type="button" variant="outline" onClick={() => navigate(-1)}>
                  Vazgeç
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
