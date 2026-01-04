
import React, { useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChatMessage } from '../types';

interface VoiceBubbleKitProps {
  messages: ChatMessage[];
  isListening: boolean;
}

const VoiceBubbleKit: React.FC<VoiceBubbleKitProps> = ({ messages, isListening }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior: 'smooth'
      });
    }
  }, [messages, isListening]);

  return (
    <div className="w-full h-full flex flex-col justify-end overflow-hidden">
      <div 
        ref={scrollRef}
        className="overflow-y-auto max-h-full flex flex-col gap-5 no-scrollbar px-2 pb-4 pt-2"
      >
        <AnimatePresence initial={false}>
          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, y: 20, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}
            >
              <div className={`
                max-w-[90%] px-6 py-4 rounded-[28px] text-[15px] font-medium leading-relaxed shadow-lg
                ${msg.role === 'user' 
                  ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white rounded-br-none shadow-cyan-500/10' 
                  : 'bg-white/5 border border-white/10 text-white/90 rounded-bl-none backdrop-blur-sm'
                }
              `}>
                {msg.text}
              </div>
              <span className="text-[9px] mt-2 opacity-30 font-bold uppercase tracking-wider mx-2">
                {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </motion.div>
          ))}
          
          {isListening && (
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex justify-start items-center gap-2 px-4 py-3 bg-cyan-400/5 rounded-2xl w-fit"
            >
               <span className="text-[10px] text-cyan-400 font-black uppercase tracking-widest">Aether is listening</span>
               <div className="flex gap-1">
                 <motion.span animate={{ opacity: [0, 1, 0] }} transition={{ repeat: Infinity, duration: 1, delay: 0 }} className="w-1 h-1 bg-cyan-400 rounded-full" />
                 <motion.span animate={{ opacity: [0, 1, 0] }} transition={{ repeat: Infinity, duration: 1, delay: 0.2 }} className="w-1 h-1 bg-cyan-400 rounded-full" />
                 <motion.span animate={{ opacity: [0, 1, 0] }} transition={{ repeat: Infinity, duration: 1, delay: 0.4 }} className="w-1 h-1 bg-cyan-400 rounded-full" />
               </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default VoiceBubbleKit;
