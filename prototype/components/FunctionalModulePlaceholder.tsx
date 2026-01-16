
import React, { useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ServiceCard, RobotState } from '../types';
import { getIcon } from '../constants';
import { Sparkles, Info, BrainCircuit, X } from 'lucide-react';
import { TypewriterText } from './VoiceDialoguePanel';
import FocusTimerWidget from './FocusTimerWidget';

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
}

const FunctionalModulePlaceholder: React.FC<FunctionalModulePlaceholderProps> = ({ 
  card, 
  aiMsg, 
  robotState, 
  onAiSpeechComplete,
  onClose,
  timerCommand,
  timerStatus,
  onTimerComplete
}) => {

  const renderContent = () => {
    if (card.type === 'timer') {
      return (
        <FocusTimerWidget 
          command={timerCommand || null} 
          timerStatus={timerStatus || 'IDLE'}
          onTimerComplete={onTimerComplete || (() => {})} 
        />
      );
    }

    // Default Placeholder for other cards
    return (
      <div className="flex flex-col items-center justify-center gap-8 w-full h-full">
         <div className="w-full max-w-sm aspect-video rounded-[32px] bg-white/5 border border-dashed border-white/10 flex flex-col items-center justify-center gap-4 group hover:bg-white/10 transition-colors">
            <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center group-hover:scale-110 transition-transform">
               <Info className="w-6 h-6 text-white/20" />
            </div>
            <p className="text-sm font-medium text-white/30 tracking-widest uppercase text-center">
              {card.type.toUpperCase()} 功能模块开发中
            </p>
         </div>
         
         <div className="grid grid-cols-2 gap-4 w-full max-w-md">
            {[1,2,3,4].map(i => (
              <div key={i} className="h-16 rounded-2xl bg-white/5 border border-white/5 animate-pulse" />
            ))}
         </div>
      </div>
    );
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="w-full h-full glass rounded-[48px] border border-white/10 shadow-2xl flex flex-col overflow-hidden bg-slate-950/30 backdrop-blur-3xl"
    >
      {/* Top Bar - Integrated AI Speech Area & Header */}
      <div className="px-10 py-8 border-b border-white/10 bg-gradient-to-r from-white/5 to-transparent flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center border border-white/10 text-white shadow-lg ${card.type === 'timer' ? 'bg-gradient-to-tr from-red-500/40 to-orange-500/40' : 'bg-gradient-to-tr from-cyan-500/40 to-indigo-500/40'}`}>
              {getIcon(card.icon)}
            </div>
            <div className="flex flex-col">
              <h2 className="text-2xl font-black tracking-tight text-white">{card.title}</h2>
              <div className="flex items-center gap-2 mt-1">
                <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-cyan-400/70">Aether System Module</span>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 glass px-4 py-2 rounded-full border border-white/10 hidden md:flex">
               <BrainCircuit className="w-3.5 h-3.5 text-cyan-400" />
               <span className="text-[9px] font-black uppercase tracking-widest text-cyan-400">System Ready</span>
            </div>
            
            <button 
              onClick={onClose}
              className="w-12 h-12 flex items-center justify-center rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 transition-all hover:scale-105 active:scale-95 text-white/40 hover:text-white"
            >
               <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        {/* Integrated AI Dialogue Area inside the Card Header */}
        <AnimatePresence mode="wait">
          {(aiMsg || robotState === 'THINKING') && (
            <motion.div 
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="bg-cyan-400/5 rounded-[32px] border border-cyan-400/20 px-8 py-5 overflow-hidden shadow-inner"
            >
               {robotState === 'THINKING' ? (
                 <div className="flex gap-2.5 items-center py-2">
                    {[1,2,3].map(i => (
                      <motion.div 
                        key={i}
                        animate={{ opacity: [0.3, 1, 0.3], scale: [1, 1.3, 1] }}
                        transition={{ repeat: Infinity, duration: 1, delay: i * 0.15 }}
                        className="w-2 h-2 bg-cyan-400 rounded-full"
                      />
                    ))}
                    <span className="text-xs font-bold text-cyan-400/40 uppercase tracking-widest ml-2">处理中...</span>
                 </div>
               ) : aiMsg ? (
                 <div className="text-[17px] font-bold text-cyan-50/90 italic leading-relaxed tracking-wide">
                    <TypewriterText text={aiMsg} onComplete={onAiSpeechComplete} />
                 </div>
               ) : null}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Dynamic Content Body */}
      <div className="flex-1 p-6 relative">
         {renderContent()}
      </div>

      <div className="px-10 py-6 border-t border-white/5 flex justify-center bg-black/40">
        <p className="text-[11px] font-bold text-white/20 uppercase tracking-[0.3em]">
          End-to-End Encrypted Interaction Layer
        </p>
      </div>
    </motion.div>
  );
};

export default FunctionalModulePlaceholder;
