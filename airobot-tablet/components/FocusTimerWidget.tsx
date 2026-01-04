
import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, Pause, Square, Timer, CheckCircle2, Sparkles } from 'lucide-react';

interface FocusTimerWidgetProps {
  command: { duration: number; task: string } | null;
  timerStatus: 'IDLE' | 'RUNNING' | 'PAUSED'; // Controlled by parent
  onTimerComplete: () => void;
}

const FocusTimerWidget: React.FC<FocusTimerWidgetProps> = ({ command, timerStatus, onTimerComplete }) => {
  const [timeLeft, setTimeLeft] = useState(0);
  const [totalTime, setTotalTime] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);
  const [taskName, setTaskName] = useState("等待指令...");
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Initialize timer when command is received and state becomes RUNNING for the first time
  useEffect(() => {
    if (command && timerStatus !== 'IDLE' && totalTime === 0) {
      const { duration, task } = command;
      setTotalTime(duration);
      setTimeLeft(duration);
      setTaskName(task || "专注时刻");
      setIsCompleted(false);
    }
  }, [command, timerStatus, totalTime]);

  // Handle Timer Logic based on timerStatus prop
  useEffect(() => {
    if (timerStatus === 'RUNNING' && timeLeft > 0) {
      timerRef.current = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            setIsCompleted(true);
            if (timerRef.current) clearInterval(timerRef.current);
            onTimerComplete();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [timerStatus, timeLeft, onTimerComplete]);

  // Reset if status goes back to IDLE
  useEffect(() => {
    if (timerStatus === 'IDLE' && !isCompleted && totalTime > 0) {
        // Reset logic if interaction closed
        setTotalTime(0);
        setTimeLeft(0);
    }
  }, [timerStatus, isCompleted, totalTime]);


  // Format time MM:SS
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const isRunning = timerStatus === 'RUNNING';
  const isPaused = timerStatus === 'PAUSED';

  // Visual state helpers
  const getGradient = () => isCompleted 
    ? "from-emerald-500 to-teal-400" 
    : isPaused ? "from-yellow-500 to-orange-500" : "from-red-500 to-orange-600";
    
  const getShadow = () => isCompleted
    ? "shadow-[0_20px_50px_rgba(16,185,129,0.5)]"
    : isPaused ? "shadow-[0_20px_50px_rgba(234,179,8,0.4)]" : "shadow-[0_20px_50px_rgba(239,68,68,0.4)]";
    
  const getStrokeColor = () => isCompleted ? "#10b981" : isPaused ? "#eab308" : "#ef4444";

  return (
    <div className="flex flex-col items-center justify-center h-full w-full relative">
      
      {/* Skeuomorphic Timer Container */}
      <motion.div 
        animate={{ scale: isCompleted ? [1, 1.05, 1] : 1 }}
        transition={{ duration: 0.5, repeat: isCompleted ? Infinity : 0, repeatDelay: 1 }}
        className={`relative w-64 h-64 rounded-full bg-gradient-to-br ${getGradient()} ${getShadow()} flex items-center justify-center border-4 border-white/10 transition-colors duration-1000`}
      >
        
        {/* Glossy Reflection */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 w-48 h-24 bg-gradient-to-b from-white/20 to-transparent rounded-full blur-[2px]" />

        {/* Inner Dial */}
        <div className="w-56 h-56 rounded-full bg-slate-900/90 shadow-inner relative flex items-center justify-center overflow-hidden">
            
            {/* Completion Burst Effect */}
            <AnimatePresence>
                {isCompleted && (
                    <motion.div 
                        initial={{ opacity: 0, scale: 0.5 }}
                        animate={{ opacity: 1, scale: 1.5 }}
                        exit={{ opacity: 0 }}
                        className="absolute inset-0 bg-emerald-500/20 blur-xl"
                    />
                )}
            </AnimatePresence>

            {/* SVG Progress Ring */}
            <svg className="absolute inset-0 w-full h-full -rotate-90 p-2" viewBox="0 0 100 100">
               {/* Background Track */}
               <circle cx="50" cy="50" r="45" fill="none" stroke="#333" strokeWidth="6" />
               {/* Progress Path */}
               <motion.circle 
                 cx="50" cy="50" r="45" 
                 fill="none" 
                 stroke={getStrokeColor()}
                 strokeWidth="6"
                 strokeLinecap="round"
                 initial={{ strokeDasharray: 283, strokeDashoffset: 283 }}
                 animate={{ strokeDashoffset: (isRunning || isPaused || isCompleted) ? (283 * (1 - timeLeft/totalTime)) : 0 }}
                 style={{ strokeDasharray: 283, strokeDashoffset: (isRunning || isPaused || isCompleted) ? (283 * (1 - timeLeft/totalTime)) : 0 }}
               />
            </svg>

            {/* Ticks */}
            {[...Array(12)].map((_, i) => (
                <div 
                  key={i} 
                  className="absolute w-1 h-2 bg-white/20"
                  style={{ 
                    top: '6px', 
                    left: 'calc(50% - 2px)', 
                    transformOrigin: '50% 108px', 
                    transform: `rotate(${i * 30}deg)` 
                  }} 
                />
            ))}

            {/* Digital Display / Success Icon */}
            <div className="flex flex-col items-center z-10">
                <AnimatePresence mode="wait">
                    {isCompleted ? (
                        <motion.div
                            key="completed"
                            initial={{ scale: 0.5, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            className="flex flex-col items-center"
                        >
                            <CheckCircle2 className="w-16 h-16 text-emerald-400 mb-2 drop-shadow-[0_0_15px_rgba(52,211,153,0.6)]" />
                            <span className="text-2xl font-bold text-white tracking-widest uppercase">Finished</span>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="timer"
                            initial={{ scale: 0.9, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            className="flex flex-col items-center"
                        >
                             <span className="text-5xl font-mono font-bold text-white tracking-wider tabular-nums drop-shadow-lg">
                                {formatTime(timeLeft)}
                            </span>
                        </motion.div>
                    )}
                </AnimatePresence>
                
                <AnimatePresence mode="wait">
                    <motion.span 
                        key={taskName}
                        initial={{ opacity: 0, y: 5 }}
                        animate={{ opacity: 1, y: 0 }}
                        className={`text-xs font-bold mt-2 uppercase tracking-widest max-w-[120px] truncate ${isCompleted ? 'text-emerald-200' : isPaused ? 'text-yellow-200' : 'text-red-200'}`}
                    >
                        {taskName}
                    </motion.span>
                </AnimatePresence>
            </div>
        </div>

        {/* Physical Button Visual */}
        <div className={`absolute -top-3 left-1/2 -translate-x-1/2 w-12 h-8 rounded-sm shadow-md border-t border-white/20 transition-colors duration-1000 ${isCompleted ? 'bg-emerald-700' : isPaused ? 'bg-yellow-600' : 'bg-red-700'}`} />
      </motion.div>

      {/* Controls / Status */}
      <div className="mt-8 flex items-center gap-6 min-h-[40px]">
         {!isRunning && !isPaused && !isCompleted && (
             <div className="flex items-center gap-2 text-white/40 bg-white/5 px-4 py-2 rounded-full border border-white/5">
                <Timer className="w-4 h-4" />
                <span className="text-xs font-bold uppercase tracking-wider">Waiting for command</span>
             </div>
         )}
         {isRunning && (
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-center gap-3 text-red-400 font-bold"
            >
               <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
               <span className="text-xs uppercase tracking-[0.2em]">Focus Mode Active</span>
            </motion.div>
         )}
         {isPaused && (
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-center gap-3 text-yellow-400 font-bold"
            >
               <Pause className="w-3 h-3 fill-yellow-400" />
               <span className="text-xs uppercase tracking-[0.2em]">Timer Paused</span>
            </motion.div>
         )}
         {isCompleted && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex items-center gap-2 text-emerald-400 font-bold"
            >
               <Sparkles className="w-4 h-4" />
               <span className="text-xs uppercase tracking-[0.2em]">Great Job!</span>
               <Sparkles className="w-4 h-4" />
            </motion.div>
         )}
      </div>
    </div>
  );
};

export default FocusTimerWidget;
