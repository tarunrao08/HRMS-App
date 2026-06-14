import api from "./api"

export interface OnboardingWorkflow {
  id: string
  employeeId: string
  employeeName: string
  employeeCode: string
  templateId: string
  templateName: string
  startDate: string
  status: string
  currentStep: number
  totalSteps: number
  completedTasks: number
  totalTasks: number
  daysInProgress?: number
  startedAt?: string
  completedAt?: string
}

export interface OnboardingTask {
  id: string
  workflowId: string
  title: string
  description?: string
  taskType: string
  status: string
  dueDate: string
  assignedTo?: string
  completedAt?: string
  remarks?: string
}

export interface OnboardingTemplate {
  id: string
  name: string
  description?: string
  totalTasks: number
}

export interface OnboardingDocumentResponse {
  id: string
  employeeId: string
  employeeName: string
  taskId?: string
  documentType: string
  documentName: string
  fileUrl: string
  fileSizeBytes?: number
  mimeType?: string
  uploadedAt: string
  verified: boolean
  verifiedBy?: string
  verifiedAt?: string
  rejectionReason?: string
}

const onboardingService = {
  getTemplates() { return api.get<OnboardingTemplate[]>("/onboarding/templates") },

  getWorkflows() { return api.get<OnboardingWorkflow[]>("/onboarding/workflows/all") },

  getWorkflow(id: string) { return api.get<OnboardingWorkflow>(`/onboarding/workflows/${id}`) },

  getWorkflowByEmployee(employeeId: string) {
    return api.get<OnboardingWorkflow>(`/onboarding/workflows/employee/${employeeId}`)
  },

  getTasks(workflowId: string) {
    return api.get<OnboardingTask[]>(`/onboarding/workflows/${workflowId}/tasks`)
  },

  completeTask(taskId: string, remarks?: string) {
    return api.patch(`/onboarding/workflows/tasks/${taskId}`, { status: "COMPLETED", notes: remarks })
  },

  initiateWorkflow(employeeId: string, templateId: string) {
    return api.post<OnboardingWorkflow>("/onboarding/workflows", { employeeId, templateId })
  },

  uploadDocument(workflowId: string, taskId: string | null, documentType: string, file: File) {
    const formData = new FormData()
    formData.append("file", file)
    formData.append("documentType", documentType)
    if (taskId) formData.append("taskId", taskId)
    return api.post<OnboardingDocumentResponse>(
      `/onboarding/workflows/${workflowId}/documents`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    )
  },

  getWorkflowDocuments(workflowId: string) {
    return api.get<OnboardingDocumentResponse[]>(`/onboarding/workflows/${workflowId}/documents`)
  },

  verifyDocument(documentId: string) {
    return api.post<OnboardingDocumentResponse>(`/onboarding/documents/${documentId}/verify`)
  },

  rejectDocument(documentId: string, reason: string) {
    return api.post<OnboardingDocumentResponse>(`/onboarding/documents/${documentId}/reject`, null, {
      params: { rejectionReason: reason }
    })
  },
}

export default onboardingService
