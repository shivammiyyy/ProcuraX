import { createContext, useContext } from "react";
import type { SessionUser } from "./types";

interface SessionContextValue {
  user: SessionUser;
  updateUser: (user: SessionUser) => void;
}

export const SessionContext = createContext<SessionContextValue | null>(null);

export function useSession(): SessionContextValue {
  const session = useContext(SessionContext);
  if (!session) throw new Error("useSession must be used within the authenticated workspace");
  return session;
}
