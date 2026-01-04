
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Mic, Command, MessageSquareText, Headphones, Hourglass, Pause, Square, Play } from 'lucide-react';
import { RobotState, ServiceCard } from '../types';

interface VoiceInputPanelProps {
  robotState: RobotState;
  onStartInteraction: () => void;
  onSimulatedSpeechEnd: (text: string) => void;
  activeCard?: ServiceCard | null;
  // Controls for timer in Focus mode
  timerStatus?: 'IDLE' | 'RUNNING' | 'PAUSED';
  onTimerControl?: (action: 'PAUSE' | 'RESUME' | 'STOP') => void;
}

const VoiceInputPanel: React.FC<VoiceInputPanelProps> = ({ 
  robotState, 
  onStartInteraction, 
  onSimulatedSpeechEnd,
  activeCard,
  timerStatus,
  onTimerControl
}) => {
  
  const isListening = robotState === 'LISTENING';
  const isThinking = robotState === 'THINKING';
  const isSpeaking = robotState === 'SPEAKING';
  
  // Active Timer Override
  const isTimerActive = timerStatus && timerStatus !== 'IDLE';

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
        
        {/* STATE: IDLE (Only if timer is NOT active) */}
        {robotState === 'IDLE' && !isTimerActive && (
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

        {/* STATE: TIMER ACTIVE (Running or Paused) */}
        {isTimerActive && (
           <motion.div
             key="timer-controls"
             initial={{ opacity: 0, y: 20 }}
             animate={{ opacity: 1, y: 0 }}
             exit={{ opacity: 0, y: 20 }}
             className="flex flex-col items-center gap-6 w-full"
           >
              {/* No Mic / Busy State Visual */}
              <div className={`relative w-20 h-20 rounded-full flex items-center justify-center border border-white/5 shadow-inner transition-colors duration-500 ${timerStatus === 'PAUSED' ? 'bg-slate-800/80' : 'bg-slate-900/80'}`}>
                  {timerStatus === 'RUNNING' && <div className="absolute inset-0 rounded-full border-t border-cyan-400/30 animate-spin-slow" />}
                  <Hourglass className={`w-8 h-8 ${timerStatus === 'PAUSED' ? 'text-white/30' : 'text-cyan-200/50'}`} />
              </div>

              {/* Manual Control Chips */}
              <div className="flex gap-4">
                 {timerStatus === 'RUNNING' && (
                    <button 
                      onClick={() => onTimerControl?.('PAUSE')}
                      className="px-6 py-3 rounded-full bg-white/5 border border-white/10 flex items-center gap-2 hover:bg-white/10 transition-colors group"
                    >
                       <Pause className="w-4 h-4 text-yellow-400 group-hover:scale-110 transition-transform" />
                       <span className="text-xs font-bold text-white/80">暂停计时</span>
                    </button>
                 )}
                 {timerStatus === 'PAUSED' && (
                    <button 
                      onClick={() => onTimerControl?.('RESUME')}
                      className="px-6 py-3 rounded-full bg-white/5 border border-white/10 flex items-center gap-2 hover:bg-white/10 transition-colors group"
                    >
                       <Play className="w-4 h-4 text-emerald-400 group-hover:scale-110 transition-transform" />
                       <span className="text-xs font-bold text-white/80">继续计时</span>
                    </button>
                 )}
                 <button 
                   onClick={() => onTimerControl?.('STOP')}
                   className="px-6 py-3 rounded-full bg-white/5 border border-white/10 flex items-center gap-2 hover:bg-red-500/20 hover:border-red-500/30 transition-colors group"
                 >
                    <Square className="w-4 h-4 text-red-400 group-hover:scale-110 transition-transform" />
                    <span className="text-xs font-bold text-white/80">结束专注</span>
                 </button>
              </div>
           </motion.div>
        )}

        {/* STATE: ACTIVE ROBOT (LISTENING / THINKING / SPEAKING) */}
        {/* Only show if NOT handling timer controls (or if speaking overrides idle) */}
        {!isTimerActive && (isListening || isThinking || isSpeaking) && (
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
