import React, { useRef, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RobotState, ServiceCard } from '../types';
import VoiceInputPanel from './VoiceInputPanel';
import { TypewriterText } from './VoiceDialoguePanel';
import { X, Sparkles, MessageSquare, Volume2 } from 'lucide-react';
import { getIcon } from '../constants';
import { useTheme } from '../theme';

const VoiceWave = ({ active }: { active: boolean }) => {
  return (
    <div className="flex items-center gap-0.5 h-3">
      {[1, 2, 3, 4].map((i) => (
        <motion.div
          key={i}
          animate={active ? {
            height: [4, 12, 4],
          } : { height: 4 }}
          transition={{
            repeat: Infinity,
            duration: 0.6,
            delay: i * 0.1,
            ease: "easeInOut"
          }}
          className="w-0.5 bg-orange-400 rounded-full"
        />
      ))}
    </div>
  );
};

export const MiniRobotAvatar = ({ robotState, className = "" }: { robotState: RobotState, className?: string }) => {
  const { theme } = useTheme();
  const [isBlinking, setIsBlinking] = useState(false);
  useEffect(() => {
    const interval = setInterval(() => {
      setIsBlinking(true);
      setTimeout(() => setIsBlinking(false), 150);
    }, 3000 + Math.random() * 2000);
    return () => clearInterval(interval);
  }, []);

  const getEyes = () => {
    if (isBlinking && robotState !== 'LISTENING' && robotState !== 'THINKING') {
      return (
        <div className="flex gap-3">
          <div className={`w-4 h-0.5 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
          <div className={`w-4 h-0.5 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
        </div>
      );
    }

    switch (robotState) {
      case 'LISTENING':
        return (
          <div className="flex gap-3">
            <motion.div animate={{ height: [2, 10, 2] }} transition={{ repeat: Infinity, duration: 1 }} className={`w-4 h-2.5 ${theme.colors.robotEyeActive} rounded-full transition-colors duration-500`} />
            <motion.div animate={{ height: [2, 10, 2] }} transition={{ repeat: Infinity, duration: 1 }} className={`w-4 h-2.5 ${theme.colors.robotEyeActive} rounded-full transition-colors duration-500`} />
          </div>
        );
      case 'THINKING':
        return (
          <div className="flex gap-3">
            <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className={`w-4 h-4 border-2 border-orange-400 rounded-full border-t-transparent transition-colors duration-500`} />
            <motion.div animate={{ rotate: -360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className={`w-4 h-4 border-2 border-orange-400 rounded-full border-t-transparent transition-colors duration-500`} />
          </div>
        );
      case 'SPEAKING':
        return (
          <div className="flex gap-3">
            <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className={`w-4 h-4 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
            <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className={`w-4 h-4 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
          </div>
        );
      default:
        return (
          <div className="flex gap-3">
            <div className={`w-4 h-4 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
            <div className={`w-4 h-4 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
          </div>
        );
    }
  };

  return (
    <div className={`w-20 h-20 rounded-[28px] ${theme.colors.robotHead} flex items-center justify-center overflow-hidden shrink-0 shadow-xl relative transition-colors duration-500 ${className}`}>
      <div className="relative z-10 flex items-center justify-center">
        {getEyes()}
      </div>
      {/* Subtle glow effect */}
      <div className={`absolute inset-0 bg-gradient-to-tr ${theme.colors.robotAuraInner} opacity-20`} />
    </div>
  );
};

interface ServiceStateSidebarProps {
  robotState: RobotState;
  chatHistory: { role: 'user' | 'ai'; text: string }[];
  onAiSpeechComplete: () => void;
  onClose: () => void;
  activeCard?: ServiceCard | null;
}

const ServiceStateSidebar: React.FC<ServiceStateSidebarProps> = ({
  robotState,
  chatHistory,
  onAiSpeechComplete,
  onClose,
  activeCard
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll logic
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [chatHistory, robotState]);

  // Poll for scroll height changes during speaking
  useEffect(() => {
    if (robotState === 'SPEAKING') {
      const interval = setInterval(() => {
        if (scrollRef.current) {
          scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
        const activeBubble = document.getElementById('active-ai-bubble');
        if (activeBubble) {
          activeBubble.scrollTop = activeBubble.scrollHeight;
        }
      }, 100);
      return () => clearInterval(interval);
    }
  }, [robotState]);

  const { theme } = useTheme();

  const getStatusText = () => {
    switch (robotState) {
      case 'LISTENING': return 'AETHER LISTENING';
      case 'THINKING': return 'AETHER THINKING';
      case 'SPEAKING': return 'AETHER SPEAKING';
      default: return 'AETHER READY';
    }
  };

  return (
    <div className={`w-full h-full flex flex-col ${theme.colors.cardBg} rounded-[32px] border ${theme.colors.cardBorder} shadow-2xl overflow-visible transition-colors duration-500`}>
      
      {/* Header Area - Anthropomorphic Design */}
      <div className={`p-3 pt-5 flex items-start justify-between border-b ${theme.colors.cardBorder} ${theme.colors.backgroundShapes} shadow-sm relative z-10 transition-colors duration-500 rounded-t-[32px]`}>
        
        {/* Large Avatar Overlapping */}
        <div className="absolute -top-6 left-6">
          <MiniRobotAvatar robotState={robotState} />
        </div>

        <div className="flex flex-col ml-24">
          <div className="flex items-center gap-2 mb-1">
            <div className={`w-6 h-6 rounded-lg flex items-center justify-center border border-white/50 shadow-sm ${activeCard?.type === 'timer' ? 'bg-amber-100 text-amber-500' : 'bg-orange-100 text-orange-500'}`}>
              {activeCard && React.cloneElement(getIcon(activeCard.icon) as React.ReactElement, { className: 'w-3.5 h-3.5' })}
            </div>
            <span className={`text-base font-extrabold ${theme.colors.textPrimary} tracking-tight`}>{activeCard?.title}</span>
          </div>
          
          <div className="flex items-center gap-2">
            <motion.div 
              animate={{ opacity: robotState !== 'IDLE' ? [0.4, 1, 0.4] : 1 }}
              transition={{ repeat: Infinity, duration: 1.5 }}
              className={`w-2 h-2 rounded-full ${robotState === 'LISTENING' ? 'bg-cyan-400' : robotState === 'THINKING' ? 'bg-orange-400' : robotState === 'SPEAKING' ? 'bg-emerald-400' : 'bg-slate-300'}`} 
            />
            <span className={`text-[10px] font-black uppercase tracking-[0.15em] ${theme.colors.textMuted}`}>
              {getStatusText()}
            </span>
          </div>
        </div>

        <button 
          onClick={(e) => {
             e.stopPropagation();
             onClose();
          }}
          className={`w-10 h-10 flex items-center justify-center rounded-full ${theme.colors.cardBg} hover:bg-slate-100 border ${theme.colors.cardBorder} transition-all hover:scale-105 active:scale-95 ${theme.colors.textMuted} hover:${theme.colors.textSecondary} cursor-pointer shadow-sm`}
        >
           <X className="w-5 h-5" />
        </button>
      </div>

      {/* Middle Area: Chat History */}
      <div 
        ref={scrollRef}
        className={`flex-1 overflow-y-auto p-6 pt-8 flex flex-col gap-6 scrollbar-hide ${theme.colors.cardBg} relative z-0 transition-colors duration-500 rounded-b-[32px]`}
      >
        <AnimatePresence mode="popLayout">
          {chatHistory.length === 0 ? (
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex-1 flex flex-col items-center justify-center text-center opacity-40 gap-3"
            >
               <MessageSquare className={`w-12 h-12 ${theme.colors.textMuted}`} />
               <span className={`text-xs font-bold uppercase tracking-widest ${theme.colors.textMuted}`}>Ready for commands</span>
            </motion.div>
          ) : (
            chatHistory.map((msg, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, y: 20, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}
              >
                {msg.role === 'ai' && (
                  <div className="flex items-center gap-2 mb-1.5 ml-1">
                    <VoiceWave active={idx === chatHistory.length - 1 && robotState === 'SPEAKING'} />
                  </div>
                )}
                
                <div className={`px-4 py-3 rounded-2xl max-w-[90%] text-sm font-medium leading-relaxed shadow-sm border relative group transition-colors duration-500 ${
                  msg.role === 'user' 
                    ? `bg-orange-50 text-orange-800 border-orange-100 rounded-tr-none` 
                    : `${theme.colors.cardBg} ${theme.colors.textPrimary} ${theme.colors.cardBorder} rounded-tl-none`
                }`}>
                  {msg.role === 'ai' && idx === chatHistory.length - 1 && robotState === 'SPEAKING' ? (
                    <div id="active-ai-bubble" className="max-h-[120px] overflow-y-auto scrollbar-hide">
                      <TypewriterText 
                        text={msg.text} 
                        onComplete={onAiSpeechComplete} 
                        speed={40} 
                      />
                    </div>
                  ) : (
                    <div className="max-h-[120px] overflow-y-auto scrollbar-hide">
                      {msg.text}
                    </div>
                  )}
                </div>
              </motion.div>
            ))
          )}
        </AnimatePresence>
        
        {robotState === 'THINKING' && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className={`flex items-center gap-2 px-4 py-2 ${theme.colors.cardBg} rounded-full border ${theme.colors.cardBorder} self-start shadow-sm transition-colors duration-500`}
          >
             <div className="flex gap-1">
                {[1,2,3].map(i => (
                  <motion.div 
                    key={i}
                    animate={{ opacity: [0.2, 1, 0.2] }}
                    transition={{ repeat: Infinity, duration: 1, delay: i * 0.15 }}
                    className={`w-1.5 h-1.5 ${theme.colors.robotEyeActive} rounded-full transition-colors duration-500`}
                  />
                ))}
             </div>
             <span className={`text-[10px] font-bold uppercase tracking-widest ${theme.colors.textMuted}`}>Thinking</span>
          </motion.div>
        )}
      </div>
    </div>
  );
};

export default ServiceStateSidebar;
