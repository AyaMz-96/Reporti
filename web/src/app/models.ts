export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface Sector {
  id: number;
  name: string;
  zone: string | null;
}

export interface CreateReportRequest {
  title: string;
  description: string;
  latitude: number;
  longitude: number;
  categoryId: number;
  sectorId: number;
  priority: PriorityLevel;
}

export type ReportStatus = 'RECEIVED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type PriorityLevel = 'URGENT' | 'NORMAL' | 'LOW';

export interface ReportResponse {
  id: number;
  title: string;
  description: string;
  status: ReportStatus;
  priority: PriorityLevel;
  latitude: number;
  longitude: number;
  categoryId: number;
  sectorId: number;
  createdAt: string;
  mediaUrls: string[];
}
export type NotificationType =
  | 'STATUS_CHANGED'
  | 'NEW_COMMENT'
  | 'ASSIGNED'
  | 'URGENT_UPDATE'
  | 'NEW_REPORT'
  | 'DISPUTE'
  | 'AGENT_REFUSAL'
  | 'SYSTEM_ALERT';


export interface AppNotification {
  id: number;
  message: string;
  createdAt: string;
  isRead: boolean;
  type?: NotificationType;
  reportId?: number;
}