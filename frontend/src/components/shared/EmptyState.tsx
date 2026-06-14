import { InboxIcon } from "lucide-react"

interface EmptyStateProps {
  title?: string
  description?: string
  action?: React.ReactNode
}

import React from "react"

export default function EmptyState({
  title = "No records found",
  description = "There are no items to display yet.",
  action,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <InboxIcon className="h-12 w-12 text-gray-300 mb-4" />
      <p className="text-lg font-medium text-gray-700">{title}</p>
      <p className="mt-1 text-sm text-muted-foreground max-w-sm">{description}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
