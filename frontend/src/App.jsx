import AppRoutes from "./routes/AppRoutes.jsx";
import { NotificationProvider } from "./context/NotificationContext.jsx";

export default function App() {
  return (
      <NotificationProvider>
        <AppRoutes />
      </NotificationProvider>
  );
}