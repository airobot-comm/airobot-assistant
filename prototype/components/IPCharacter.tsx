
import React, { useEffect, useState } from 'react';
import { motion, Variants, AnimatePresence } from 'framer-motion';
import { RobotState } from '../types';
import { useTheme } from '../theme';

interface IPCharacterProps {
  state: RobotState;
}

const IPCharacter: React.FC<IPCharacterProps> = ({ state }) => {
  const { theme } = useTheme();
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [isBlinking, setIsBlinking] = useState(false);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
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
    FOCUS: { rotate: 0, scale: 1, transition: { duration: 0.5 } }
  };

  const getEyes = () => {
    if (isBlinking && state !== 'LISTENING' && state !== 'THINKING' && state !== 'FOCUS') {
      return (
        <div className="flex gap-14">
           <div className={`w-16 h-2 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
           <div className={`w-16 h-2 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
        </div>
      );
    }

    switch (state) {
      case 'LISTENING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ height: [12, 56, 12], scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5 }} className={`w-16 h-14 ${theme.colors.robotEyeActive} rounded-full transition-colors duration-500`} />
            <motion.div animate={{ height: [12, 56, 12], scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1.5 }} className={`w-16 h-14 ${theme.colors.robotEyeActive} rounded-full transition-colors duration-500`} />
          </div>
        );
      case 'THINKING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className={`w-16 h-16 border-[8px] border-orange-400 rounded-full border-t-transparent transition-colors duration-500`} />
            <motion.div animate={{ rotate: -360 }} transition={{ repeat: Infinity, duration: 2, ease: "linear" }} className={`w-16 h-16 border-[8px] border-orange-400 rounded-full border-t-transparent transition-colors duration-500`} />
          </div>
        );
      case 'SPEAKING':
        return (
          <div className="flex gap-14">
            <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className={`w-16 h-16 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
            <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 0.3 }} className={`w-16 h-16 ${theme.colors.robotEyeDefault} rounded-full transition-colors duration-500`} />
          </div>
        );
      case 'FOCUS':
        return (
          <div className="flex gap-14">
            <motion.div 
               initial={{ width: 64, height: 64 }}
               animate={{ height: 8, width: 70 }} 
               className={`${theme.colors.robotEyeFocus} rounded-full transition-colors duration-500`} 
            />
            <motion.div 
               initial={{ width: 64, height: 64 }}
               animate={{ height: 8, width: 70 }} 
               className={`${theme.colors.robotEyeFocus} rounded-full transition-colors duration-500`} 
            />
          </div>
        );
      default: 
        return (
          <div className="flex gap-14">
            <div className={`w-16 h-16 ${theme.colors.robotEyeDefault} rounded-full relative overflow-hidden transition-colors duration-500`}>
               <motion.div 
                 className="absolute w-5 h-5 bg-white rounded-full top-3 right-4 opacity-80"
                 animate={{ x: faceX * 0.1, y: faceY * 0.1 }} 
               />
            </div>
            <div className={`w-16 h-16 ${theme.colors.robotEyeDefault} rounded-full relative overflow-hidden transition-colors duration-500`}>
               <motion.div 
                 className="absolute w-5 h-5 bg-white rounded-full top-3 right-4 opacity-80"
                 animate={{ x: faceX * 0.1, y: faceY * 0.1 }} 
               />
            </div>
          </div>
        );
    }
  };

  const getFaceFeatures = () => {
    return (
      <div className="flex flex-col items-center gap-6">
        {getEyes()}
        
        {/* Blush - Anthropomorphic touch */}
        <div className="flex justify-between w-full px-12 -mt-8 opacity-40">
          <div className="w-8 h-4 bg-rose-300 rounded-full blur-[4px]" />
          <div className="w-8 h-4 bg-rose-300 rounded-full blur-[4px]" />
        </div>

        {/* Mouth - Expressive */}
        <motion.div 
          animate={{ 
            height: state === 'SPEAKING' ? [4, 12, 4] : (state === 'THINKING' ? 2 : 6),
            width: state === 'SPEAKING' ? [20, 40, 20] : (state === 'THINKING' ? 30 : 40),
            borderRadius: state === 'THINKING' ? "2px" : "20px"
          }}
          transition={{ duration: 0.3 }}
          className={`${theme.colors.robotEyeDefault} opacity-60`}
        />
      </div>
    );
  };

  return (
    <div className={`relative ${theme.sizes.robotWidth} ${theme.sizes.robotHeight} flex flex-col items-center justify-center pointer-events-none perspective-1000`}>
      
      {/* Background Aura - Structured Halo */}
      <motion.div 
        animate={{ 
          scale: state === 'FOCUS' ? 0.8 : [1, 1.1, 1],
          opacity: state === 'FOCUS' ? 0.2 : [0.5, 0.7, 0.5] 
        }}
        transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
        className={`absolute w-[500px] h-[500px] rounded-full bg-gradient-to-tr ${theme.colors.robotAuraOuter} blur-[60px] -z-10 transition-colors duration-500`}
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
        className="relative z-10 flex flex-col items-center"
      >
        {/* Head Shell - Soft Claymorphism / Metallic Look */}
        <div className={`w-[420px] h-[310px] ${theme.colors.robotHead} rounded-[120px] shadow-[0_30px_60px_-12px_rgba(0,0,0,0.15),inset_0_-12px_24px_rgba(0,0,0,0.05),inset_0_4px_8px_rgba(255,255,255,0.2)] relative flex items-center justify-center overflow-visible border border-sky-400/60 transition-colors duration-500`}>
           
           {/* Ears - Anthropomorphic detail */}
           <div className={`absolute -left-4 top-1/2 -translate-y-1/2 w-8 h-20 ${theme.colors.robotHead} rounded-l-full border-l border-y border-sky-400/50 shadow-[-2px_0_8px_rgba(0,0,0,0.03)]`} />
           <div className={`absolute -right-4 top-1/2 -translate-y-1/2 w-8 h-20 ${theme.colors.robotHead} rounded-r-full border-r border-y border-sky-400/50 shadow-[2px_0_8px_rgba(0,0,0,0.03)]`} />

           {/* Specular Highlight - Glossy effect */}
           <div className="absolute top-6 left-16 w-28 h-10 bg-gradient-to-b from-white/30 to-transparent rounded-full blur-[2px] opacity-30" />

           <motion.div 
             variants={antennaVariants}
             animate={state}
             className="absolute -top-16 left-28 w-6 h-20 bg-gradient-to-b from-slate-300 to-slate-400 rounded-full origin-bottom shadow-sm border border-slate-400/20"
           >
              <div className={`w-14 h-14 rounded-full absolute -top-6 -left-4 shadow-lg border-4 ${theme.colors.robotHead} ${state === 'FOCUS' ? theme.colors.robotAntennaFocus : theme.colors.robotAntennaDefault} transition-colors duration-500 flex items-center justify-center`}>
                 <div className="w-4 h-4 bg-white/60 rounded-full absolute top-1.5 left-2.5" />
              </div>
           </motion.div>
           <motion.div 
             variants={antennaVariants}
             animate={state}
             className="absolute -top-16 right-28 w-6 h-20 bg-gradient-to-b from-slate-300 to-slate-400 rounded-full origin-bottom shadow-sm border border-slate-400/20"
           >
              <div className={`w-14 h-14 rounded-full absolute -top-6 -left-4 shadow-lg border-4 ${theme.colors.robotHead} ${state === 'FOCUS' ? theme.colors.robotAntennaFocus : theme.colors.robotEyeActive} transition-colors duration-500 flex items-center justify-center`}>
                 <div className="w-4 h-4 bg-white/60 rounded-full absolute top-1.5 left-2.5" />
              </div>
           </motion.div>

           {/* Inner Face Area - Deep Soft Inset */}
           <div className={`absolute inset-9 rounded-[100px] ${theme.colors.robotFace} shadow-[inset_0_8px_24px_rgba(0,0,0,0.08),0_2px_4px_rgba(255,255,255,0.1)] border border-sky-300/50 transition-colors duration-500`} />

           <motion.div 
             animate={{ x: faceX, y: faceY }}
             transition={{ type: "spring", stiffness: 70, damping: 25 }}
             className="relative z-30 flex flex-col items-center"
           >
              {getFaceFeatures()}
           </motion.div>
        </div>

        {/* Neck & Collar - Weakened/Subtle Base */}
        <div className="relative -mt-2 z-0 flex flex-col items-center opacity-100 scale-100">
          <div className="w-16 h-12 bg-slate-300/50 shadow-inner rounded-sm" />
          <div className={`w-48 h-12 ${theme.colors.robotHead} rounded-t-[40px] shadow-md border-t border-x border-sky-400/50`} />
        </div>

        {/* Soft Floor Shadow - Enhanced */}
        <div className="absolute -bottom-14 left-1/2 -translate-x-1/2 w-[360px] h-[24px] bg-black/10 blur-[24px] rounded-full" />
      </motion.div>
    </div>
  );
};

export default IPCharacter;
