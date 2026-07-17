import { useState } from "react";
import { useNavigate } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login } from "@/api/auth";
import { ApiError } from "@/api/client";
import { useAuth } from "@/auth/auth-context";

export function LoginPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setAuth({ token: data.accessToken, user: data.user });
      navigate("/", { replace: true });
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.message : "Giriş başarısız";
      toast.error(message);
    },
  });

  return (
    <div className="bg-muted/30 flex min-h-screen items-center justify-center p-4">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          mutation.mutate({ email, password });
        }}
        className="bg-background w-full max-w-sm space-y-4 rounded-lg border p-6 shadow-sm"
      >
        <div className="space-y-1 text-center">
          <h1 className="text-lg font-semibold">Giriş yap</h1>
          <p className="text-muted-foreground text-sm">Kurumsal hesabınızla oturum açın</p>
        </div>

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

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Giriş yapılıyor…" : "Giriş yap"}
        </Button>
      </form>
    </div>
  );
}
