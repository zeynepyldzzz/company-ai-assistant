import { Outlet } from "react-router";
import { Header } from "./header";
import { Sidebar } from "./sidebar";

export function AppLayout() {
  return (
    <div className="bg-background flex h-screen">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 overflow-auto px-9 pt-8 pb-12">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
