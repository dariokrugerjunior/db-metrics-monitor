import { createContext, useContext, useState, type ReactNode } from "react";

const CONNECTED_KEY = "db-metrics-connected";
const DB_URL_KEY = "db-metrics-dburl";

interface AuthContextValue {
  isConnected: boolean;
  connectedDbUrl: string | null;
  connect: (dbUrl: string) => void;
  disconnect: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readSession(): { isConnected: boolean; connectedDbUrl: string | null } {
  return {
    isConnected: sessionStorage.getItem(CONNECTED_KEY) === "true",
    connectedDbUrl: sessionStorage.getItem(DB_URL_KEY),
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState(readSession);

  function connect(dbUrl: string) {
    sessionStorage.setItem(CONNECTED_KEY, "true");
    sessionStorage.setItem(DB_URL_KEY, dbUrl);
    setState({ isConnected: true, connectedDbUrl: dbUrl });
  }

  function disconnect() {
    sessionStorage.removeItem(CONNECTED_KEY);
    sessionStorage.removeItem(DB_URL_KEY);
    setState({ isConnected: false, connectedDbUrl: null });
  }

  return (
    <AuthContext.Provider value={{ ...state, connect, disconnect }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
