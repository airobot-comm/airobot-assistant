
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Mic, Command, MessageSquareText, Headphones } from 'lucide-react';
import { RobotState, ServiceCard } from '../types';

interface VoiceInputPanelProps {
  robotState: RobotState;
  onStartInteraction: () => void;
  onSimulatedSpeechEnd: (text: string) => void;
  activeCard?: ServiceCard | null;
}

const VoiceInputPanel: React.FC<VoiceInputPanelProps> = ({ 
  robotState, 
  onStartInteraction, 
  onSimulatedSpeechEnd,
  activeCard
}) => {
  
  const isListening = robotState === 'LISTENING';
  const isThinking = robotState === 'THINKING';
  const isSpeaking = robotState === 'SPEAKING';

  // Determine suggestions based on active card
  const getSuggestions = () => {
    if (activeCard?.type === 'timer') {
      return ["1分钟写日记", "30秒休息", "5分钟发呆"];
    }
    return ["讲个故事", "明天天气", "设个闹钟"];
  };

  const suggestions = getSuggestions();

  return (
    <div className="w-full max-w-2xl flex flex-col items-center justify-center relative min-h-[160px]">
      
      <AnimatePresence mode="wait">
        
        {/* STATE: IDLE */}
        {robotState === 'IDLE' && (
          <motion.div
            key="idle"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9 }}
            className="flex flex-col items-center gap-6"
          >
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={onStartInteraction}
              className="relative w-24 h-24 rounded-full bg-slate-900 flex items-center justify-center border-2 border-white/10 group shadow-2xl overflow-hidden"
            >
              <motion.div 
                animate={{ scale: [1, 1.4], opacity: [0.2, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="absolute inset-0 rounded-full border border-indigo-400"
              />
              <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/20 to-transparent" />
              <Mic className="w-8 h-8 text-white z-10 group-hover:scale-110 transition-transform drop-shadow-lg" />
            </motion.button>
            
            <div className="flex flex-col items-center gap-2">
               <motion.div 
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 className="px-8 py-3 glass rounded-full flex items-center gap-3 border border-white/5 shadow-xl"
               >
                  <MessageSquareText className="w-4 h-4 text-cyan-300" />
                  <span className="text-[13px] text-white/70 font-bold uppercase tracking-[0.2em]">叫名字，开始对话</span>
               </motion.div>
            </div>
          </motion.div>
        )}

        {/* STATE: ACTIVE (LISTENING / THINKING / SPEAKING) */}
        {(isListening || isThinking || isSpeaking) && (
          <motion.div
            key="active-status"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="flex flex-col items-center gap-6 w-full"
          >
             {/* Status Badge & Mic Animation */}
             <div className="flex items-center gap-4 glass px-10 py-5 rounded-full border border-white/10 shadow-2xl">
                {isListening && (
                   <div className="flex items-center gap-4">
                      <div className="flex gap-1.5 items-center">
                        {[1,2,3,4,5].map(i => (
                          <motion.div 
                            key={i}
                            animate={{ height: [8, 24, 8] }}
                            transition={{ duration: 0.5, repeat: Infinity, delay: i * 0.1 }}
                            className="w-1.5 bg-cyan-400 rounded-full"
                          />
                        ))}
                      </div>
                      <span className="text-[14px] font-black uppercase tracking-[0.2em] text-cyan-400">请说话...</span>
                   </div>
                )}
                {isThinking && (
                   <div className="flex items-center gap-3">
                      <Command className="w-5 h-5 text-indigo-400 animate-spin-slow" />
                      <span className="text-[14px] font-black uppercase tracking-[0.2em] text-indigo-400">思考中</span>
                   </div>
                )}
                {isSpeaking && (
                   <div className="flex items-center gap-4">
                      <div className="flex gap-1 items-center">
                        {[1,2,3].map(i => (
                          <motion.div 
                            key={i}
                            animate={{ scale: [1, 1.5, 1], opacity: [0.3, 1, 0.3] }}
                            transition={{ duration: 0.8, repeat: Infinity, delay: i * 0.2 }}
                            className="w-1.5 h-1.5 bg-white rounded-full"
                          />
                        ))}
                      </div>
                      <span className="text-[14px] font-black uppercase tracking-[0.2em] text-white/90">正在播报回复</span>
                   </div>
                )}
             </div>

             {/* Quick Simulation Trigger */}
             {isListening && (
               <div className="flex gap-3">
                  {suggestions.map(txt => (
                    <button 
                      key={txt}
                      onClick={() => onSimulatedSpeechEnd(txt)}
                      className="px-5 py-2 rounded-full bg-white/5 border border-white/5 text-[11px] text-white/40 hover:bg-white/10 transition-colors hover:text-white hover:border-white/20"
                    >
                      "{txt}"
                    </button>
                  ))}
               </div>
             )}
          </motion.div>
        )}

      </AnimatePresence>
    </div>
  );
};

export default VoiceInputPanel;
