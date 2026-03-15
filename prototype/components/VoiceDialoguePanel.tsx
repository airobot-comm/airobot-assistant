
import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RobotState, InteractionType } from '../types';
import { BrainCircuit, Volume2, X } from 'lucide-react';
import { useTheme } from '../theme';

interface VoiceDialoguePanelProps {
  robotState: RobotState;
  userMsg: string | null;
  aiMsg: string | null;
  onAiSpeechComplete: () => void;
  onClose: () => void;
  layoutMode: InteractionType;
}

export const TypewriterText: React.FC<{ text: string; speed?: number; onComplete?: () => void }> = ({ text, speed = 30, onComplete }) => {
  const [displayedText, setDisplayedText] = useState("");
  const isFinishedRef = useRef(false);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    setDisplayedText("");
    isFinishedRef.current = false;
    let i = 0;
    const interval = setInterval(() => {
      setDisplayedText(text.slice(0, i + 1));
      i++;
      if (i >= text.length) {
        clearInterval(interval);
        isFinishedRef.current = true;
        if (onCompleteRef.current) onCompleteRef.current();
      }
    }, speed);
    return () => clearInterval(interval);
  }, [text, speed]);

  return <>{isFinishedRef.current ? text : displayedText}</>;
};

const VoiceDialoguePanel: React.FC<VoiceDialoguePanelProps> = ({ robotState, userMsg, aiMsg, onAiSpeechComplete, onClose, layoutMode }) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const { theme } = useTheme();

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [aiMsg, robotState]);

  useEffect(() => {
    if (robotState === 'SPEAKING') {
      const interval = setInterval(() => {
        if (scrollRef.current) {
          scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
      }, 100);
      return () => clearInterval(interval);
    }
  }, [robotState]);

  // Only show the floating bubble in CHAT mode
  const showFloatingBubble = layoutMode === 'CHAT' && (aiMsg || robotState === 'THINKING');

  return (
    <div className="absolute inset-0 z-[200] pointer-events-none">
      <div className={`absolute top-0 left-1/2 -translate-x-1/2 ${theme.sizes.robotWidth} ${theme.sizes.robotHeight}`}>
        <AnimatePresence mode="wait">
          {showFloatingBubble && (
            <motion.div
              key="ai-floating-bubble"
              initial={{ opacity: 0, scale: 0.8, x: 500, y: 60 }}
              animate={{ 
                opacity: 1, 
                scale: 1, 
                x: 550, 
                y: 60,
                transition: { type: "spring", stiffness: 100, damping: 20 }
              }}
              exit={{ opacity: 0, scale: 0.8, x: 580 }}
              className="absolute z-[300] w-[340px] pointer-events-auto"
            >
              <div className={`relative ${theme.colors.cardBg} backdrop-blur-xl border ${theme.colors.cardBorder} rounded-[32px] shadow-[0_20px_40px_-10px_rgba(0,0,0,0.1)] overflow-hidden flex flex-col transition-colors duration-500`}>
                {/* Header info */}
                <div className={`px-6 py-3 border-b ${theme.colors.cardBorder} flex items-center justify-between ${theme.colors.backgroundShapes} transition-colors duration-500`}>
                   <div className="flex items-center gap-2">
                     <BrainCircuit className={`w-4 h-4 ${theme.colors.accent}`} />
                     <span className={`text-[11px] font-bold uppercase tracking-widest ${theme.colors.textMuted}`}>Aether System</span>
                   </div>
                   {robotState === 'SPEAKING' && (
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4].map((i) => (
                          <motion.div
                            key={i}
                            animate={{ height: ["4px", "12px", "4px"] }}
                            transition={{ repeat: Infinity, duration: 0.8, delay: i * 0.15, ease: "easeInOut" }}
                            className={`w-1 ${theme.colors.accentBg} rounded-full`}
                          />
                        ))}
                      </div>
                   )}
                   <button onClick={onClose} className={`w-8 h-8 flex items-center justify-center rounded-full ${theme.colors.cardBg} hover:bg-slate-100 transition-colors`}>
                      <X className={`w-4 h-4 ${theme.colors.textMuted} hover:${theme.colors.textSecondary}`} />
                   </button>
                </div>

                {/* Content */}
                <div 
                  ref={scrollRef}
                  className="px-6 py-5 max-h-[140px] overflow-y-auto no-scrollbar scroll-smooth"
                >
                  {robotState === 'THINKING' ? (
                    <div className="flex gap-2 items-center py-2">
                      {[1,2,3].map(i => (
                        <motion.div 
                          key={i}
                          animate={{ scale: [1, 1.2, 1], opacity: [0.4, 1, 0.4] }}
                          transition={{ repeat: Infinity, duration: 1, delay: i * 0.2 }}
                          className={`w-2 h-2 ${theme.colors.accentBg} rounded-full`}
                        />
                      ))}
                    </div>
                  ) : aiMsg ? (
                    <div className={`text-[16px] font-medium ${theme.colors.textPrimary} leading-relaxed transition-colors duration-500`}>
                       <TypewriterText text={aiMsg} onComplete={onAiSpeechComplete} />
                    </div>
                  ) : null}
                </div>
                
                {robotState === 'SPEAKING' && (
                  <div className={`h-[2px] w-full ${theme.colors.cardBorder} overflow-hidden transition-colors duration-500`}>
                    <motion.div 
                      animate={{ x: ["-100%", "100%"] }}
                      transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
                      className={`h-full w-1/2 bg-gradient-to-r from-transparent via-${theme.colors.accentBg.split('-')[1]}-400 to-transparent`}
                    />
                  </div>
                )}
              </div>
              
              {/* Connector Tail - Pointing left towards avatar */}
              <div className={`absolute top-[40px] left-[-8px] w-5 h-5 ${theme.colors.cardBg} backdrop-blur-xl border-l border-b ${theme.colors.cardBorder} rotate-45 -z-10 shadow-[-4px_4px_10px_rgba(0,0,0,0.02)] transition-colors duration-500`} />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default VoiceDialoguePanel;
