import { RouterProvider } from "react-router";
import { router } from "./routes";
import { Toaster } from "./components/ui/sonner";
import { useEffect } from "react";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { ConnectPage } from "./pages/ConnectPage";

function AppContent() {
  const { isConnected } = useAuth();
  return isConnected ? <RouterProvider router={router} /> : <ConnectPage />;
}

export default function App() {
  useEffect(() => {
    // Force dark mode
    document.documentElement.classList.add("dark");
  }, []);

  return (
    <AuthProvider>
      <AppContent />
      <Toaster />
    </AuthProvider>
  );
}