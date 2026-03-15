
import React from 'react';
import { motion } from 'framer-motion';
import { Bell, Check, X, Clock } from 'lucide-react';
import { useTheme } from '../theme';

interface AlarmWidgetProps {
  onDismiss: () => void;
}

const AlarmWidget: React.FC<AlarmWidgetProps> = ({ onDismiss }) => {
  const { theme } = useTheme();
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.9, y: 20 }}
      className="w-full max-w-md relative"
    >
      {/* Alarm Clock Bells - Softened */}
      <div className={`absolute -top-6 left-12 w-20 h-10 ${theme.colors.backgroundShapes} rounded-t-full shadow-sm border-b-2 ${theme.colors.cardBorder} transform -rotate-12 transition-colors duration-500`} />
      <div className={`absolute -top-6 right-12 w-20 h-10 ${theme.colors.backgroundShapes} rounded-t-full shadow-sm border-b-2 ${theme.colors.cardBorder} transform rotate-12 transition-colors duration-500`} />
      <div className={`absolute -top-8 left-1/2 -translate-x-1/2 w-4 h-8 ${theme.colors.backgroundShapes} rounded-full transition-colors duration-500`} />

      {/* Main Body - Soft Theme */}
      <div className={`${theme.colors.cardBg} rounded-[48px] p-6 shadow-[0_20px_40px_-10px_rgba(0,0,0,0.1)] border ${theme.colors.cardBorder} relative overflow-hidden transition-colors duration-500`}>
        {/* Glass Face */}
        <div className={`${theme.colors.backgroundShapes} rounded-[32px] p-8 shadow-inner border ${theme.colors.cardBorder} relative transition-colors duration-500`}>
          {/* Reflection */}
          <div className="absolute top-2 left-4 w-3/4 h-1/2 bg-gradient-to-b from-white/60 to-transparent rounded-full blur-[2px] pointer-events-none" />

          <div className="relative z-10 flex flex-col items-center">
            {/* Header */}
            <div className={`flex items-center gap-2 ${theme.colors.accent} mb-6 font-bold uppercase tracking-widest text-sm transition-colors duration-500`}>
              <Bell className="w-5 h-5 animate-bounce" />
              <span>AI Alarm</span>
            </div>

            {/* Time Display */}
            <div className="relative mb-8">
               <div className={`text-7xl font-black ${theme.colors.textPrimary} tracking-tighter font-mono transition-colors duration-500`}>
                 07:30
               </div>
               <div className={`text-lg ${theme.colors.textMuted} font-bold text-center mt-2 italic transition-colors duration-500`}>
                 Rise and Shine!
               </div>
            </div>

            {/* Progress Bar */}
            <div className={`w-full h-3 ${theme.colors.cardBg} rounded-full mb-8 overflow-hidden transition-colors duration-500`}>
              <motion.div 
                initial={{ width: 0 }}
                animate={{ width: "100%" }}
                transition={{ duration: 10 }}
                className={`h-full ${theme.colors.accentBg} rounded-full`} 
              />
            </div>

            {/* Controls */}
            <div className="flex w-full gap-4">
              <button 
                onClick={onDismiss}
                className={`flex-1 py-4 rounded-2xl ${theme.colors.cardBg} hover:${theme.colors.backgroundShapes} ${theme.colors.textMuted} font-bold shadow-sm active:translate-y-0.5 active:shadow-none transition-all flex items-center justify-center gap-2 border ${theme.colors.cardBorder}`}
              >
                <X className="w-5 h-5" />
                CANCEL
              </button>
              <button 
                onClick={onDismiss}
                className={`flex-1 py-4 rounded-2xl ${theme.colors.accentBg} hover:opacity-90 text-white font-bold shadow-md shadow-orange-500/20 active:translate-y-0.5 active:shadow-none transition-all flex items-center justify-center gap-2 border border-transparent`}
              >
                <Check className="w-5 h-5" />
                DONE
              </button>
            </div>
          </div>
        </div>
      </div>
      
      {/* Legs - Softened */}
      <div className={`absolute -bottom-4 left-16 w-6 h-10 ${theme.colors.backgroundShapes} rounded-full -z-10 transform -rotate-15 shadow-sm transition-colors duration-500`} />
      <div className={`absolute -bottom-4 right-16 w-6 h-10 ${theme.colors.backgroundShapes} rounded-full -z-10 transform rotate-15 shadow-sm transition-colors duration-500`} />
    </motion.div>
  );
};

export default AlarmWidget;
