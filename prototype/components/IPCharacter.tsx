
import React, { useEffect, useState } from 'react';
import { motion, Variants, AnimatePresence } from 'framer-motion';
import { RobotState } from '../types';

interface IPCharacterProps {
  state: RobotState;
  statusTip?: string;
}

const IPCharacter: React.FC<IPCharacterProps> = ({ state, statusTip }) => {
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [isBlinking, setIsBlinking] = useState(false);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      // During FOCUS, reduce head tracking movement for a "steady" look
      const dampener = state === 'FOCUS' ? 0.2 : 1;
      const x = ((e.clientX / window.innerWidth) * 2 - 1) * dampener;
      const y = ((e.clientY / window.innerHeight) * 2 - 1) * dampener;
      setMousePos({ x, y });
    };

    const blinkInterval = setInterval(() => {
      setIsBlinking(true);
      setTimeout(() => setIsBlinking(false), 150);
    }, 4000);

    window.addEventListener('mousemove', handleMouseMove);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      clearInterval(blinkInterval);
    };
  }, [state]);

  const headX = mousePos.x * 12;
  const headY = mousePos.y * 8;
  const faceX = mousePos.x * 15;
  const faceY = mousePos.y * 12;

  const antennaVariants: Variants = {
    IDLE: { rotate: [0, 8, 0, -8, 0], transition: { duration: 5, repeat: Infinity } },
    LISTENING: { scale: 1.15, transition: { duration: 0.3, repeat: Infinity, repeatType: "reverse" } },
    THINKING: { rotate: 360, transition: { duration: 2, repeat: Infinity, ease: "linear" } },
    SPEAKING: { y: [0, -5, 0], transition: { duration: 0.4, repeat: Infinity } },
    FOCUS: { rotate: 0, scale: 1, transition: { duration: 0.5 } } // Steady antenna
  };

  const getEyes = () => {
    if (isBlinking && state !== 'LISTENING' && state !== 'THINKING' && state !== 'FOCUS') {
      return (
        <div className="flex gap-14">
           <div className="w-16 h-2 bg-cyan-300 rounded-full shadow-[0_0_20px_#22d3ee]" />
           <div className="w-16 h-2 bg-cyan-300 rounded-full shadow-[0_0_20px_#22d3ee]" />
        </div>
      );
    }

    switch (state) {
      case 'LISTENING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ height: [12, 56, 12], scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5 }} className="w-16 h-14 bg-white rounded-full blur-[1px] shadow-[0_0_40px_#fff]" />
            <motion.div animate={{ height: [12, 56, 12], scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5 }} className="w-16 h-14 bg-white rounded-full blur-[1px] shadow-[0_0_40px_#fff]" />
          </div>
        );
      case 'THINKING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className="w-16 h-16 border-[8px] border-cyan-400 rounded-full border-t-transparent shadow-[0_0_25px_rgba(34,211,238,0.4)]" />
            <motion.div animate={{ rotate: -360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className="w-16 h-16 border-[8px] border-cyan-400 rounded-full border-t-transparent shadow-[0_0_25px_rgba(34,211,238,0.4)]" />
          </div>
        );
      case 'SPEAKING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ scale: [1, 1.3, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className="w-16 h-16 bg-white rounded-full shadow-[0_0_50px_rgba(255,255,255,0.9)]" />
            <motion.div animate={{ scale: [1, 1.3, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className="w-16 h-16 bg-white rounded-full shadow-[0_0_50px_rgba(255,255,255,0.9)]" />
          </div>
        );
      case 'FOCUS':
        return (
          <div className="flex gap-14">
            {/* Zen/Focus Eyes: Horizontal lines with a slight glow, calm look */}
            <motion.div 
               initial={{ width: 64, height: 64 }}
               animate={{ height: 8, width: 70 }} 
               className="bg-cyan-200 rounded-full shadow-[0_0_15px_#a5f3fc]" 
            />
            <motion.div 
               initial={{ width: 64, height: 64 }}
               animate={{ height: 8, width: 70 }} 
               className="bg-cyan-200 rounded-full shadow-[0_0_15px_#a5f3fc]" 
            />
          </div>
        );
      default: 
        return (
          <div className="flex gap-14">
            <div className="w-16 h-16 bg-gradient-to-br from-indigo-500 to-blue-700 rounded-full shadow-[0_15px_50px_rgba(59,130,246,0.5)] relative overflow-hidden border-2 border-white/30">
               <motion.div 
                 className="absolute w-6 h-6 bg-white rounded-full top-3 right-5 opacity-90 blur-[1px]"
                 animate={{ x: faceX * 0.1, y: faceY * 0.1 }} 
               />
               <div className="absolute inset-0 bg-gradient-to-tr from-white/10 to-transparent" />
            </div>
            <div className="w-16 h-16 bg-gradient-to-br from-indigo-500 to-blue-700 rounded-full shadow-[0_15px_50px_rgba(59,130,246,0.5)] relative overflow-hidden border-2 border-white/30">
               <motion.div 
                 className="absolute w-6 h-6 bg-white rounded-full top-3 right-5 opacity-90 blur-[1px]"
                 animate={{ x: faceX * 0.1, y: faceY * 0.1 }} 
               />
               <div className="absolute inset-0 bg-gradient-to-tr from-white/10 to-transparent" />
            </div>
          </div>
        );
    }
  };

  return (
    <div className="relative w-[600px] h-[450px] flex flex-col items-center justify-center pointer-events-none perspective-1000">
      
      {/* Background Aura */}
      <motion.div 
        animate={{ 
          scale: state === 'FOCUS' ? 0.8 : [1, 1.25, 1],
          opacity: state === 'FOCUS' ? 0.05 : [0.1, 0.25, 0.1] 
        }}
        transition={{ duration: 10, repeat: Infinity }}
        className="absolute w-[500px] h-[500px] rounded-full bg-cyan-500/20 blur-[140px] -z-10"
      />

      {/* Main Structure */}
      <motion.div
        animate={{ 
          x: headX, 
          y: headY + (state === 'IDLE' ? Math.sin(Date.now() / 1500) * 15 : 0),
          rotateX: -mousePos.y * 5,
          rotateY: mousePos.x * 8
        }}
        transition={{ type: "spring", stiffness: 35, damping: 30 }}
        className="relative z-10"
      >
        {/* Dynamic Status Tip Bubble (IDLE or FOCUS) */}
        <AnimatePresence mode="wait">
          {(state === 'IDLE' || state === 'FOCUS') && statusTip && (
            <motion.div
              key={statusTip}
              initial={{ opacity: 0, scale: 0.5, x: 200, y: -40 }}
              animate={{ 
                opacity: 1, 
                scale: 1, 
                x: 230, 
                y: -60,
                transition: { type: "spring", stiffness: 100, damping: 15 } 
              }}
              exit={{ opacity: 0, scale: 0.8, x: 250, transition: { duration: 0.2 } }}
              className="absolute z-[100] flex items-center"
            >
              <div className="bg-white/10 backdrop-blur-md px-5 py-2.5 rounded-2xl border border-white/10 shadow-2xl flex items-center gap-2">
                 <div className={`w-1.5 h-1.5 rounded-full animate-pulse ${state === 'FOCUS' ? 'bg-red-400' : 'bg-cyan-400'}`} />
                 <span className="text-[13px] font-bold text-white/80 whitespace-nowrap tracking-wider">
                   {statusTip}
                 </span>
              </div>
              <div className="absolute bottom-[-6px] left-[10px] w-3 h-3 bg-white/10 border-r border-b border-white/10 rotate-45" />
            </motion.div>
          )}
        </AnimatePresence>

        {/* Head Shell */}
        <div className="w-[420px] h-[300px] bg-slate-950/90 backdrop-blur-3xl rounded-[110px] border-[1px] border-white/10 shadow-2xl relative flex items-center justify-center overflow-visible">
           
           <motion.div 
             variants={antennaVariants}
             animate={state}
             className="absolute -top-16 left-24 w-4 h-20 bg-slate-800 rounded-full origin-bottom border border-white/5"
           >
              <div className={`w-10 h-10 rounded-full absolute -top-5 -left-3 shadow-[0_0_25px_#22d3ee] border-4 border-white/40 ${state === 'FOCUS' ? 'bg-red-400 shadow-red-500/50' : 'bg-cyan-400'}`} />
           </motion.div>
           <motion.div 
             variants={antennaVariants}
             animate={state}
             className="absolute -top-16 right-24 w-4 h-20 bg-slate-800 rounded-full origin-bottom border border-white/5"
           >
              <div className={`w-10 h-10 rounded-full absolute -top-5 -left-3 shadow-[0_0_25px_#6366f1] border-4 border-white/40 ${state === 'FOCUS' ? 'bg-red-400 shadow-red-500/50' : 'bg-indigo-500'}`} />
           </motion.div>

           <div className="absolute inset-5 rounded-[90px] bg-black/50 border border-white/5 shadow-inner" />

           <motion.div 
             animate={{ x: faceX, y: faceY }}
             transition={{ type: "spring", stiffness: 70, damping: 25 }}
             className="relative z-30 flex flex-col items-center"
           >
              {getEyes()}
              
              {state === 'SPEAKING' && (
                <motion.div 
                  initial={{ scaleX: 0.1, opacity: 0 }}
                  animate={{ scaleX: [0.1, 2.5, 0.1], opacity: 1 }}
                  transition={{ repeat: Infinity, duration: 0.25 }}
                  className="w-16 h-4 bg-white rounded-full mt-12 shadow-[0_0_30px_white]" 
                />
              )}
           </motion.div>
        </div>

        {/* Shadow */}
        <div className="absolute -bottom-16 left-1/2 -translate-x-1/2 w-[340px] h-[30px] bg-black/60 blur-[40px] rounded-full" />
      </motion.div>
    </div>
  );
};

export default IPCharacter;
