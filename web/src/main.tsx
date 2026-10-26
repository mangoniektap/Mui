import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import App from "./App";
import { TooltipProvider } from "@/components/ui/tooltip";
import "./index.css";

const stored = localStorage.getItem("mui-theme");
const dark =
  stored === "dark" ||
  (!stored && window.matchMedia("(prefers-color-scheme: dark)").matches);
if (dark) document.documentElement.classList.add("dark");

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <TooltipProvider delayDuration={200}>
      <App />
    </TooltipProvider>
  </StrictMode>
);
