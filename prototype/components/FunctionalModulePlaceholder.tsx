
import React, { useMemo, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ServiceCard, RobotState } from '../types';
import { getIcon } from '../constants';
import { Sparkles, Info, BrainCircuit, X } from 'lucide-react';
import { TypewriterText } from './VoiceDialoguePanel';
import FocusTimerWidget from './FocusTimerWidget';
import KnowledgeQuizWidget from './KnowledgeQuizWidget';
import InteractivePodcastWidget from './InteractivePodcastWidget';
import { useTheme } from '../theme';

interface FunctionalModulePlaceholderProps {
  card: ServiceCard;
  aiMsg: string | null;
  robotState: RobotState;
  onAiSpeechComplete: () => void;
  onClose: () => void;
  // Props for specific widgets
  timerCommand?: { duration: number; task: string } | null;
  timerStatus?: 'IDLE' | 'RUNNING' | 'PAUSED';
  onTimerComplete?: () => void;
  onTimerControl?: (action: 'PAUSE' | 'RESUME' | 'STOP') => void;
  // Quiz Props
  currentQuizCardId?: string;
  onQuizCardChange?: (cardId: string) => void;
  // Podcast Props
  currentPodcastId?: string;
  podcastStatus?: 'PLAYING' | 'PAUSED' | 'IDLE';
  onPodcastControl?: (action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREV') => void;
}

const ScrollingText: React.FC<{ text: string; onComplete: () => void }> = ({ text, onComplete }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const { theme } = useTheme();

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const observer = new MutationObserver(() => {
      container.scrollLeft = container.scrollWidth;
    });

    observer.observe(container, { childList: true, subtree: true, characterData: true });

    return () => observer.disconnect();
  }, []);

  return (
    <div 
      ref={containerRef}
      className="w-full overflow-hidden whitespace-nowrap flex items-center"
      style={{ scrollBehavior: 'smooth' }}
    >
      <span className={`text-sm font-bold ${theme.colors.textPrimary} italic tracking-wide pr-4 transition-colors duration-500`}>
        <TypewriterText text={text} onComplete={onComplete} speed={80} />
      </span>
    </div>
  );
};

const FunctionalModulePlaceholder: React.FC<FunctionalModulePlaceholderProps> = ({ 
  card, 
  aiMsg, 
  robotState, 
  onAiSpeechComplete,
  onClose,
  timerCommand,
  timerStatus,
  onTimerComplete,
  onTimerControl,
  currentQuizCardId,
  onQuizCardChange,
  currentPodcastId,
  podcastStatus,
  onPodcastControl
}) => {
  const { theme } = useTheme();
  // Resilience against null card during exit animations
  if (!card) return null;

  const renderContent = () => {
    if (card.type === 'timer') {
      return (
        <FocusTimerWidget 
          command={timerCommand || null} 
          timerStatus={timerStatus || 'IDLE'}
          onTimerComplete={onTimerComplete || (() => {})} 
          onTimerControl={onTimerControl}
        />
      );
    }

    if (card.type === 'quiz') {
      return (
        <KnowledgeQuizWidget 
          currentCardId={currentQuizCardId}
          onCardChange={onQuizCardChange}
        />
      );
    }

    if (card.type === 'podcast') {
      return (
        <InteractivePodcastWidget
          currentPodcastId={currentPodcastId}
          podcastStatus={podcastStatus}
          onPodcastControl={onPodcastControl}
        />
      );
    }

    // Default Placeholder for other cards
    return (
      <motion.div 
        className={`relative flex flex-col items-center justify-center w-[420px] h-[620px] ${theme.colors.cardBg} backdrop-blur-2xl rounded-[56px] shadow-[0_30px_60px_-15px_rgba(0,0,0,0.1),inset_0_0_0_1px_rgba(255,255,255,0.5)] border ${theme.colors.cardBorder} overflow-hidden transition-colors duration-500`}
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        transition={{ type: "spring", stiffness: 300, damping: 25 }}
      >
         <div className={`w-24 h-24 rounded-full ${theme.colors.backgroundShapes} flex items-center justify-center shadow-[inset_0_10px_20px_rgba(0,0,0,0.05)] border ${theme.colors.cardBorder} mb-8 transition-colors duration-500`}>
            {React.cloneElement(getIcon(card.icon) as React.ReactElement, { className: `w-12 h-12 ${theme.colors.textMuted}` })}
         </div>
         <h2 className={`text-3xl font-black ${theme.colors.textPrimary} tracking-tight mb-3 transition-colors duration-500`}>{card.title}</h2>
         <p className={`text-sm font-bold ${theme.colors.textMuted} tracking-widest uppercase text-center px-8 transition-colors duration-500`}>
           模块开发中...
         </p>
         
         <div className="grid grid-cols-2 gap-4 w-full px-12 mt-16">
            {[1,2,3,4].map(i => (
              <div key={i} className={`h-16 rounded-2xl ${theme.colors.backgroundShapes} border ${theme.colors.cardBorder} shadow-sm animate-pulse transition-colors duration-500`} />
            ))}
         </div>
      </motion.div>
    );
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className="w-full h-full flex flex-col relative items-center justify-center"
    >
      {/* Dynamic Content Body - Main Area (Borderless, Skeuomorphic) */}
      <div className="relative flex items-center justify-center w-full h-full max-w-4xl translate-y-[10%] scale-110">
         {renderContent()}
      </div>
    </motion.div>
  );
};

export default FunctionalModulePlaceholder;
