import { useState } from "react";
import { useNavigate } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login, verifyTwoFactor } from "@/api/auth";
import { API_BASE, ApiError } from "@/api/client";
import { useAuth } from "@/auth/auth-context";
import { ThemeToggle } from "@/components/theme-toggle";

function LoginShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-primary flex min-h-screen items-center justify-center p-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <div className="bg-card w-full max-w-[380px] space-y-5 rounded-2xl border p-7 shadow-xl">
        <div className="flex flex-col items-center gap-3 text-center">
          <img src="/logo-wordmark.png" alt="Yaşar Bilgi" className="h-14 max-w-full object-contain" />
          <p className="text-muted-foreground text-sm">Ofis Asistanı</p>
        </div>
        {children}
      </div>
    </div>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [challengeToken, setChallengeToken] = useState<string | null>(null);
  // C-12 (#120): yeni olusturulan admin turu hesaplar authenticator app'ine
  // henuz kayitli degil - once QR kodu gostermeliyiz (bkz. AuthDtos.enrollmentRequired).
  const [enrollmentRequired, setEnrollmentRequired] = useState(false);

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      if ("twoFactorRequired" in data) {
        setChallengeToken(data.challengeToken);
        setEnrollmentRequired(data.enrollmentRequired);
        return;
      }
      setAuth({ accessToken: data.accessToken, refreshToken: data.refreshToken, user: data.user });
      navigate("/", { replace: true });
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.message : "Giriş başarısız";
      toast.error(message);
    },
  });

  const verifyMutation = useMutation({
    mutationFn: () => verifyTwoFactor(challengeToken!, code),
    onSuccess: (data) => {
      setAuth({ accessToken: data.accessToken, refreshToken: data.refreshToken, user: data.user });
      navigate("/", { replace: true });
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.message : "Kod doğrulanamadı";
      toast.error(message);
    },
  });

  if (challengeToken) {
    return (
      <LoginShell>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            verifyMutation.mutate();
          }}
          className="space-y-4"
        >
          <div className="space-y-1 text-center">
            <h2 className="text-base font-semibold">İki faktörlü doğrulama</h2>
            <p className="text-muted-foreground text-sm">
              {enrollmentRequired
                ? "Hesabınız için henüz kimlik doğrulama uygulaması kurulmamış. Aşağıdaki QR kodu Google Authenticator (veya benzeri bir uygulama) ile tarayıp size verilen 6 haneli kodu girin."
                : "Kimlik doğrulama uygulamanızdaki 6 haneli kodu girin"}
            </p>
          </div>

          {enrollmentRequired && (
            <div className="flex justify-center">
              <img
                src={`${API_BASE}/auth/2fa/qr?challengeToken=${encodeURIComponent(challengeToken)}`}
                alt="İki faktörlü doğrulama QR kodu"
                className="h-40 w-40 rounded-md border"
              />
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="code">Doğrulama kodu</Label>
            <Input
              id="code"
              inputMode="numeric"
              autoComplete="one-time-code"
              required
              autoFocus
              value={code}
              onChange={(event) => setCode(event.target.value)}
            />
          </div>

          <Button type="submit" className="w-full" disabled={verifyMutation.isPending}>
            {verifyMutation.isPending ? "Doğrulanıyor…" : "Doğrula"}
          </Button>
          <Button
            type="button"
            variant="ghost"
            className="w-full"
            onClick={() => {
              setChallengeToken(null);
              setCode("");
              setEnrollmentRequired(false);
            }}
          >
            Geri dön
          </Button>
        </form>
      </LoginShell>
    );
  }

  return (
    <LoginShell>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          loginMutation.mutate({ email, password });
        }}
        className="space-y-4"
      >
        <div className="space-y-2">
          <Label htmlFor="email">E-posta</Label>
          <Input
            id="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">Parola</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>

        <Button type="submit" className="w-full" disabled={loginMutation.isPending}>
          {loginMutation.isPending ? "Giriş yapılıyor…" : "Giriş yap"}
        </Button>
      </form>
    </LoginShell>
  );
}
