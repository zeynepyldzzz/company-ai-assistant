import { useQuery } from "@tanstack/react-query";
import { getMe } from "@/api/auth";
import { useAuth } from "./auth-context";

export function useMe() {
  const { token } = useAuth();

  return useQuery({
    queryKey: ["me", token],
    queryFn: () => getMe(token!),
    enabled: Boolean(token),
  });
}
