import { useState } from "react";
import { Route, Routes } from "react-router-dom";
import { SplashScreen } from "./components/layout/SplashScreen";
import { PublicLayout } from "./components/layout/PublicLayout";
import { AppLayout } from "./components/layout/AppLayout";
import { ProtectedRoute } from "./components/routing/ProtectedRoute";
import { Landing } from "./pages/Landing";
import { About } from "./pages/About";
import { Dashboard } from "./pages/Dashboard";
import { EmployeeList } from "./pages/EmployeeList";
import { EmployeeDetail } from "./pages/EmployeeDetail";
import { Notifications } from "./pages/Notifications";
import { Profile } from "./pages/Profile";
import { Login } from "./pages/Login";
import { Register } from "./pages/Register";
import { NotFound } from "./pages/NotFound";

export default function App() {
  const [splashDone, setSplashDone] = useState(false);

  return (
    <>
      {!splashDone && <SplashScreen onDone={() => setSplashDone(true)} />}
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Landing />} />
          <Route path="/about" element={<About />} />
        </Route>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/employees" element={<EmployeeList />} />
            <Route path="/employees/:id" element={<EmployeeDetail />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/profile" element={<Profile />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}
