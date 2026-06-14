import { useNavigate } from "react-router-dom"
import { ShieldOff } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function ForbiddenPage() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-background text-center px-4">
      <div className="flex h-20 w-20 items-center justify-center rounded-full bg-destructive/10">
        <ShieldOff className="h-10 w-10 text-destructive" />
      </div>

      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Access Denied</h1>
        <p className="text-muted-foreground max-w-sm">
          You don't have permission to view this page. Contact your HR administrator if you think this is a mistake.
        </p>
      </div>

      <div className="flex gap-3">
        <Button variant="outline" onClick={() => navigate(-1)}>
          Go Back
        </Button>
        <Button onClick={() => navigate("/dashboard", { replace: true })}>
          Go to Dashboard
        </Button>
      </div>
    </div>
  )
}
