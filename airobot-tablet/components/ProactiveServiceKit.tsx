
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ServiceCard } from '../types';
import { getIcon } from '../constants';
import { ArrowRight, Sparkles } from 'lucide-react';

interface ProactiveServiceKitProps {
  cards: ServiceCard[];
  onCardClick?: (card: ServiceCard) => void;
}

const ProactiveServiceKit: React.FC<ProactiveServiceKitProps> = ({ cards, onCardClick }) => {
  return (
    <div className="flex flex-col gap-4 w-full">
      <AnimatePresence mode="popLayout">
        {cards.map((card) => {
          return (
            <motion.div
              key={card.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              whileHover={{ 
                x: 8,
                backgroundColor: "rgba(255, 255, 255, 0.08)",
              }}
              onClick={() => onCardClick?.(card)}
              className="
                group w-full glass p-6 rounded-[36px] flex items-center gap-5 cursor-pointer relative overflow-hidden
                transition-all active:scale-95 border border-white/10 hover:border-cyan-500/30
              "
            >
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-500 flex items-center justify-center text-white shadow-xl group-hover:shadow-cyan-500/30 transition-shadow">
                {getIcon(card.icon)}
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                   <h3 className="text-base font-bold text-white tracking-tight">{card.title}</h3>
                   <Sparkles className="w-3 h-3 text-cyan-400 opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
                <p className="text-xs text-white/40 leading-relaxed truncate">{card.content}</p>
              </div>

              <div className="absolute right-6 opacity-20 group-hover:opacity-100 group-hover:translate-x-1 transition-all">
                <ArrowRight className="w-5 h-5 text-cyan-400" />
              </div>

              {/* Progress timer bar at the bottom of the card */}
              <div className="absolute bottom-0 left-0 h-[2px] bg-cyan-500/20 w-full overflow-hidden">
                <motion.div 
                   key={card.id + "-timer"}
                   initial={{ width: 0 }}
                   animate={{ width: "100%" }}
                   transition={{ duration: 10, ease: "linear" }}
                   className="h-full bg-cyan-400"
                />
              </div>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
};

export default ProactiveServiceKit;
