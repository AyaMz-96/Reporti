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