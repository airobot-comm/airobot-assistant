
export type RobotState = 'IDLE' | 'LISTENING' | 'THINKING' | 'SPEAKING' | 'HAPPY' | 'SLEEPING' | 'FOCUS';

export type ActiveMode = 'DEFAULT' | 'ALARM';

export type InteractionType = 'CHAT' | 'CARD';

export interface ServiceCard {
  id: string;
  type: 'weather' | 'schedule' | 'news' | 'music' | 'health' | 'chat' | 'podcast' | 'game' | 'alarm' | 'timer';
  title: string;
  content: string;
  statusTip: string; // The tip that appears in the bubble near robot's head
  icon: string;
  isFixed?: boolean;
  rotation?: number;
  offsetX?: number;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  timestamp: number;
}
