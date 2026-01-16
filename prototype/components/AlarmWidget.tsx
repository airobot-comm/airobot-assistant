
import React from 'react';
import { motion } from 'framer-motion';
import { Bell, Check, X, Clock } from 'lucide-react';

interface AlarmWidgetProps {
  onDismiss: () => void;
}

const AlarmWidget: React.FC<AlarmWidgetProps> = ({ onDismiss }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.9, y: 20 }}
      className="w-full max-w-md bg-indigo-900/60 backdrop-blur-xl border border-white/20 rounded-[40px] p-6 shadow-2xl relative overflow-hidden"
    >
      {/* Background Decor */}
      <div className="absolute -top-10 -right-10 w-32 h-32 bg-cyan-500/20 rounded-full blur-2xl" />
      <div className="absolute -bottom-10 -left-10 w-32 h-32 bg-purple-500/20 rounded-full blur-2xl" />

      <div className="relative z-10 flex flex-col items-center">
        {/* Header */}
        <div className="flex items-center gap-2 text-indigo-200 mb-6 bg-black/20 px-4 py-1.5 rounded-full">
          <Clock className="w-4 h-4" />
          <span className="text-xs font-bold tracking-widest uppercase">Alarm Set</span>
        </div>

        {/* Time Display */}
        <div className="relative mb-8">
           <div className="text-7xl font-bold text-white tracking-tighter drop-shadow-[0_0_15px_rgba(255,255,255,0.3)]">
             07:30
           </div>
           <div className="text-lg text-indigo-300 font-medium text-center mt-1">
             Wake up & Shine
           </div>
        </div>

        {/* Dynamic Circle Visual */}
        <div className="w-full h-1 bg-white/10 rounded-full mb-8 overflow-hidden">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: "100%" }}
            transition={{ duration: 10 }}
            className="h-full bg-gradient-to-r from-cyan-400 to-purple-400" 
          />
        </div>

        {/* Controls */}
        <div className="flex w-full gap-4">
          <button 
            onClick={onDismiss}
            className="flex-1 py-4 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/10 text-white font-semibold transition-all flex items-center justify-center gap-2 group"
          >
            <X className="w-5 h-5 group-hover:scale-110 transition-transform" />
            Cancel
          </button>
          <button 
            onClick={onDismiss}
            className="flex-1 py-4 rounded-2xl bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 text-white font-bold shadow-lg shadow-cyan-500/30 transition-all flex items-center justify-center gap-2 group"
          >
            <Check className="w-5 h-5 group-hover:scale-110 transition-transform" />
            Done
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default AlarmWidget;
