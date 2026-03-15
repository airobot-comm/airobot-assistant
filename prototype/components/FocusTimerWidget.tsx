
import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, Pause, Square, Timer, CheckCircle2, Sparkles } from 'lucide-react';
import { useTheme } from '../theme';

interface FocusTimerWidgetProps {
  command: { duration: number; task: string } | null;
  timerStatus: 'IDLE' | 'RUNNING' | 'PAUSED'; // Controlled by parent
  onTimerComplete: () => void;
  onTimerControl?: (action: 'PAUSE' | 'RESUME' | 'STOP') => void;
}

const FocusTimerWidget: React.FC<FocusTimerWidgetProps> = ({ command, timerStatus, onTimerComplete, onTimerControl }) => {
  const { theme } = useTheme();
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
    ? "from-emerald-400 to-teal-300" 
    : isPaused ? "from-amber-400 to-orange-300" : "from-orange-400 to-amber-400";
    
  const getShadow = () => isCompleted
    ? "shadow-[0_20px_50px_-10px_rgba(16,185,129,0.3)]"
    : isPaused ? "shadow-[0_20px_50px_-10px_rgba(245,158,11,0.3)]" : "shadow-[0_20px_50px_-10px_rgba(249,115,22,0.3)]";
    
  const getStrokeColor = () => isCompleted ? "#10b981" : isPaused ? "#f59e0b" : "#f97316";

  return (
    <div className="flex flex-col items-center justify-center h-full w-full relative">
      
      {/* Skeuomorphic Timer Container - Soft Theme */}
      <motion.div 
        animate={{ 
          scale: isCompleted ? [1, 1.05, 1] : 1,
          boxShadow: isRunning 
            ? ["0 20px 50px -10px rgba(249,115,22,0.3)", "0 20px 60px -5px rgba(249,115,22,0.4)", "0 20px 50px -10px rgba(249,115,22,0.3)"]
            : isPaused ? "0 20px 50px -10px rgba(245,158,11,0.3)" : isCompleted ? "0 20px 50px -10px rgba(16,185,129,0.3)" : "0 20px 40px -10px rgba(0,0,0,0.05)"
        }}
        transition={{ duration: 2, repeat: Infinity }}
        className={`relative w-96 h-96 rounded-full bg-gradient-to-br ${getGradient()} flex items-center justify-center transition-colors duration-1000 p-5 border border-white/50`}
      >
        
        {/* Rotating Highlight */}
        {isRunning && (
          <motion.div 
            animate={{ rotate: 360 }}
            transition={{ duration: 10, repeat: Infinity, ease: "linear" }}
            className="absolute inset-0 rounded-full bg-gradient-to-tr from-white/30 to-transparent pointer-events-none"
          />
        )}

        {/* Glossy Reflection */}
        <div className="absolute top-5 left-1/2 -translate-x-1/2 w-56 h-24 bg-gradient-to-b from-white/60 to-transparent rounded-full blur-[4px] pointer-events-none z-20" />

        {/* Inner Dial - Soft White */}
        <div className={`w-full h-full rounded-full ${theme.colors.timerBg} shadow-[inset_0_10px_20px_rgba(0,0,0,0.05)] relative flex items-center justify-center overflow-hidden border-4 border-slate-50 transition-colors duration-500`}>
            
            {/* Completion Burst Effect */}
            <AnimatePresence>
                {isCompleted && (
                    <motion.div 
                        initial={{ opacity: 0, scale: 0.5 }}
                        animate={{ opacity: 1, scale: 2.5 }}
                        exit={{ opacity: 0 }}
                        className="absolute inset-0 bg-emerald-100/50 blur-2xl"
                    />
                )}
            </AnimatePresence>

            {/* Pulsing Glow for Inner Dial */}
            {isRunning && (
              <motion.div 
                animate={{ opacity: [0.05, 0.15, 0.05] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="absolute inset-0 bg-orange-400/20 blur-xl"
              />
            )}

            {/* SVG Progress Ring */}
            <svg className="absolute inset-0 w-full h-full -rotate-90 p-6" viewBox="0 0 100 100">
               {/* Background Track */}
               <circle cx="50" cy="50" r="42" fill="none" className={`${theme.colors.timerRingBg} transition-colors duration-500`} strokeWidth="8" />
               {/* Progress Path */}
               <motion.circle 
                 cx="50" cy="50" r="42" 
                 fill="none" 
                 className={`${isCompleted ? 'stroke-emerald-400' : isPaused ? theme.colors.timerRingPaused : theme.colors.timerRingActive} transition-colors duration-500`}
                 strokeWidth="8"
                 strokeLinecap="round"
                 initial={{ strokeDasharray: 264, strokeDashoffset: 264 }}
                 animate={{ strokeDashoffset: (isRunning || isPaused || isCompleted) ? (264 * (1 - timeLeft/totalTime)) : 0 }}
                 style={{ strokeDasharray: 264, strokeDashoffset: (isRunning || isPaused || isCompleted) ? (264 * (1 - timeLeft/totalTime)) : 0 }}
               />
            </svg>

            {/* Ticks */}
            {[...Array(60)].map((_, i) => (
                <div 
                  key={i} 
                  className={`absolute rounded-full ${i % 5 === 0 ? 'w-1.5 h-5 bg-slate-300' : 'w-0.5 h-2.5 bg-slate-200'}`}
                  style={{ 
                    top: '24px', 
                    left: 'calc(50% - 0.75px)', 
                    transformOrigin: '50% 150px', 
                    transform: `rotate(${i * 6}deg)` 
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
                            <CheckCircle2 className="w-24 h-24 text-emerald-500 mb-2" />
                            <span className="text-3xl font-black text-emerald-600 tracking-widest uppercase">DONE!</span>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="timer"
                            initial={{ scale: 0.9, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            className="flex flex-col items-center"
                        >
                             <motion.span 
                                animate={isRunning ? { scale: [1, 1.02, 1] } : {}}
                                transition={{ duration: 1, repeat: Infinity }}
                                className="text-8xl font-mono font-black text-slate-700 tracking-widest tabular-nums"
                             >
                                {formatTime(timeLeft)}
                            </motion.span>
                        </motion.div>
                    )}
                </AnimatePresence>
                
                <AnimatePresence mode="wait">
                    <motion.span 
                        key={taskName}
                        initial={{ opacity: 0, y: 5 }}
                        animate={{ opacity: 1, y: 0 }}
                        className={`text-base font-bold mt-6 uppercase tracking-widest max-w-[200px] truncate px-5 py-2 rounded-full bg-slate-50 border border-slate-100 ${isCompleted ? 'text-emerald-500' : isPaused ? 'text-amber-500' : 'text-orange-500'}`}
                    >
                        {taskName}
                    </motion.span>
                </AnimatePresence>
            </div>
        </div>

        {/* Physical Top Button - Softened */}
        <div className={`absolute -top-8 left-1/2 -translate-x-1/2 w-24 h-12 rounded-t-2xl shadow-md border-t-2 border-white/50 transition-all duration-500 ${isCompleted ? 'bg-emerald-400' : isPaused ? 'bg-amber-400' : 'bg-orange-400'} ${isRunning ? 'translate-y-2' : ''}`}>
           <div className="absolute top-2 left-1/2 -translate-x-1/2 w-12 h-1.5 bg-white/40 rounded-full" />
        </div>
        
        {/* Side Knobs - Softened */}
        <div className="absolute top-1/2 -left-5 -translate-y-1/2 w-8 h-20 bg-slate-200 rounded-l-xl border-r-2 border-slate-300 shadow-md flex flex-col justify-around py-4">
           {[1,2,3].map(i => <div key={i} className="w-full h-1 bg-slate-300" />)}
        </div>
        <div className="absolute top-1/2 -right-5 -translate-y-1/2 w-8 h-20 bg-slate-200 rounded-r-xl border-l-2 border-slate-300 shadow-md flex flex-col justify-around py-4">
           {[1,2,3].map(i => <div key={i} className="w-full h-1 bg-slate-300" />)}
        </div>
      </motion.div>

      {/* Controls / Status */}
      <div className="mt-16 flex flex-col items-center gap-10 min-h-[140px] w-full max-w-lg">
         {/* Status Indicators */}
         <div className="flex items-center gap-6">
            {!isRunning && !isPaused && !isCompleted && (
                <div className="flex items-center gap-2 text-slate-400 bg-white px-6 py-3 rounded-full border border-slate-100 shadow-sm">
                    <Timer className="w-5 h-5" />
                    <span className="text-sm font-bold uppercase tracking-wider">Waiting for command</span>
                </div>
            )}
            {isRunning && (
                <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center gap-3 text-orange-500 font-bold bg-white px-6 py-3 rounded-full border border-orange-100 shadow-sm"
                >
                <div className="w-2.5 h-2.5 rounded-full bg-orange-500 animate-pulse" />
                <span className="text-sm uppercase tracking-widest">Focus Mode Active</span>
                </motion.div>
            )}
            {isPaused && (
                <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center gap-3 text-amber-500 font-bold bg-white px-6 py-3 rounded-full border border-amber-100 shadow-sm"
                >
                <Pause className="w-4 h-4 fill-amber-500" />
                <span className="text-sm uppercase tracking-widest">Timer Paused</span>
                </motion.div>
            )}
            {isCompleted && (
                <motion.div 
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex items-center gap-2 text-emerald-500 font-bold bg-white px-6 py-3 rounded-full border border-emerald-100 shadow-sm"
                >
                <Sparkles className="w-5 h-5" />
                <span className="text-sm uppercase tracking-widest">Great Job!</span>
                <Sparkles className="w-5 h-5" />
                </motion.div>
            )}
         </div>

         {/* Physical-style Control Buttons - Soft Theme */}
         {(isRunning || isPaused) && (
            <div className="flex gap-8">
                {isRunning && (
                    <motion.button 
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => onTimerControl?.('PAUSE')}
                        className="w-16 h-16 rounded-2xl bg-white border border-slate-100 flex items-center justify-center group shadow-md hover:shadow-lg transition-all"
                    >
                        <Pause className="w-7 h-7 text-amber-500 fill-amber-500 group-hover:scale-110 transition-transform" />
                    </motion.button>
                )}
                {isPaused && (
                    <motion.button 
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => onTimerControl?.('RESUME')}
                        className="w-16 h-16 rounded-2xl bg-white border border-slate-100 flex items-center justify-center group shadow-md hover:shadow-lg transition-all"
                    >
                        <Play className="w-7 h-7 text-emerald-500 fill-emerald-500 group-hover:scale-110 transition-transform" />
                    </motion.button>
                )}
                <motion.button 
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => onTimerControl?.('STOP')}
                    className="w-16 h-16 rounded-2xl bg-white border border-slate-100 flex items-center justify-center group shadow-md hover:shadow-lg transition-all"
                >
                    <Square className="w-7 h-7 text-red-500 fill-red-500 group-hover:scale-110 transition-transform" />
                </motion.button>
            </div>
         )}
      </div>
    </div>
  );
};

export default FocusTimerWidget;
