import { createBrowserRouter } from "react-router";
import { Navigate } from "react-router";
import { MainLayout } from "./components/MainLayout";
import { Locks } from "./pages/Locks";
import { Queries } from "./pages/Queries";
import { Connections } from "./pages/Connections";
import { Overview } from "./pages/Overview";
import { History } from "./pages/History";
import { AiAnalysis } from "./pages/AiAnalysis";
import { Intelligence } from "./pages/Intelligence";
import { Settings } from "./pages/Settings";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: MainLayout,
    children: [
      { index: true, element: <Navigate to="/overview" replace /> },
      { path: "locks", Component: Locks },
      { path: "queries", Component: Queries },
      { path: "connections", Component: Connections },
      { path: "overview", Component: Overview },
      { path: "intelligence", Component: Intelligence },
      { path: "history", Component: History },
      { path: "ai-analysis", Component: AiAnalysis },
      { path: "settings", Component: Settings },
    ],
  },
]);
