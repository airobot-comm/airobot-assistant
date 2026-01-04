
import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RobotState, InteractionType } from '../types';
import { BrainCircuit, Volume2, X } from 'lucide-react';

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
        if (onComplete) onComplete();
      }
    }, speed);
    return () => clearInterval(interval);
  }, [text, speed, onComplete]);

  return <>{displayedText || (isFinishedRef.current ? text : "")}</>;
};

const VoiceDialoguePanel: React.FC<VoiceDialoguePanelProps> = ({ robotState, userMsg, aiMsg, onAiSpeechComplete, onClose, layoutMode }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior: 'smooth'
      });
    }
  }, [aiMsg, robotState]);

  // Only show the floating bubble in CHAT mode
  const showFloatingBubble = layoutMode === 'CHAT' && (aiMsg || robotState === 'THINKING');

  return (
    <div className="absolute inset-0 z-[200] pointer-events-none">
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[450px]">
        <AnimatePresence mode="wait">
          {showFloatingBubble && (
            <motion.div
              key="ai-floating-bubble"
              initial={{ opacity: 0, scale: 0.8, x: 480, y: 60 }}
              animate={{ 
                opacity: 1, 
                scale: 1, 
                // 头像容器宽600, 居中. 头像本体宽420.
                // 左边缘X=90, 右边缘X=510.
                // 设置 x=520 确保气泡起始位置在头像右边缘之外，完全不遮挡头像。
                x: 520, 
                y: 60,
                transition: { type: "spring", stiffness: 100, damping: 20 }
              }}
              exit={{ opacity: 0, scale: 0.8, x: 550 }}
              className="absolute z-[300] w-[340px] pointer-events-auto"
            >
              <div className="relative glass bg-slate-900/85 backdrop-blur-3xl border border-white/20 rounded-[40px] shadow-2xl overflow-hidden flex flex-col glow-indigo">
                {/* Header info */}
                <div className="px-8 py-4 border-b border-white/10 flex items-center justify-between bg-white/5">
                   <div className="flex items-center gap-2">
                     <BrainCircuit className="w-4 h-4 text-cyan-400" />
                     <span className="text-[11px] font-black uppercase tracking-[0.2em] text-cyan-400">Aether System</span>
                   </div>
                   {robotState === 'SPEAKING' && (
                      <Volume2 className="w-4 h-4 text-white/40 animate-pulse" />
                   )}
                   <button onClick={onClose} className="w-8 h-8 flex items-center justify-center rounded-full bg-white/5 hover:bg-white/10 transition-colors">
                      <X className="w-4 h-4 opacity-40 hover:opacity-100" />
                   </button>
                </div>

                {/* Content */}
                <div 
                  ref={scrollRef}
                  className="px-8 py-6 max-h-[260px] overflow-y-auto no-scrollbar"
                >
                  {robotState === 'THINKING' ? (
                    <div className="flex gap-2.5 items-center py-4">
                      {[1,2,3].map(i => (
                        <motion.div 
                          key={i}
                          animate={{ scale: [1, 1.4, 1], opacity: [0.3, 1, 0.3] }}
                          transition={{ repeat: Infinity, duration: 1, delay: i * 0.2 }}
                          className="w-2.5 h-2.5 bg-cyan-400 rounded-full shadow-[0_0_10px_rgba(34,211,238,0.5)]"
                        />
                      ))}
                    </div>
                  ) : aiMsg ? (
                    <div className="text-[17px] font-bold text-white/95 leading-relaxed tracking-wide">
                       <TypewriterText text={aiMsg} onComplete={onAiSpeechComplete} />
                    </div>
                  ) : null}
                </div>
                
                {robotState === 'SPEAKING' && (
                  <div className="h-[3px] w-full bg-white/5 overflow-hidden">
                    <motion.div 
                      animate={{ x: ["-100%", "100%"] }}
                      transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
                      className="h-full w-2/3 bg-gradient-to-r from-transparent via-cyan-400 to-transparent"
                    />
                  </div>
                )}
              </div>
              
              {/* Connector Tail - Pointing left towards avatar */}
              <div className="absolute top-[50px] left-[-10px] w-6 h-6 bg-slate-900/85 backdrop-blur-3xl border-l border-b border-white/20 rotate-45 -z-10" />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default VoiceDialoguePanel;
