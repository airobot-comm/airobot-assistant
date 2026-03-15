import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTheme } from '../theme';
import { ChevronLeft, ChevronRight, Leaf, Bird } from 'lucide-react';

export interface KnowledgeCard {
  id: string;
  title: string;
  category: 'plant' | 'animal';
  image: string;
  description: string;
  funFact: string;
}

export const KNOWLEDGE_CARDS: KnowledgeCard[] = [
  {
    id: 'k1',
    title: '向日葵 (Sunflower)',
    category: 'plant',
    image: 'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
    description: '向日葵是一种大型的一年生植物，以其巨大的黄色花朵而闻名。它们有一个奇妙的特性叫做“向光性”，也就是花盘会随着太阳的移动而转动。',
    funFact: '向日葵的花盘实际上是由成百上千朵小花组成的！'
  },
  {
    id: 'k2',
    title: '大熊猫 (Giant Panda)',
    category: 'animal',
    image: 'https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
    description: '大熊猫是中国的国宝，主要栖息在四川、陕西和甘肃的山区。它们虽然属于食肉目，但99%的食物都是竹子。',
    funFact: '大熊猫一天要花10到16个小时吃竹子，每天能吃掉12到38公斤！'
  },
  {
    id: 'k3',
    title: '捕蝇草 (Venus Flytrap)',
    category: 'plant',
    image: 'https://images.unsplash.com/photo-1613143030835-42010892019c?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
    description: '捕蝇草是一种著名的食虫植物。它的叶子边缘有规则的刺毛，当昆虫连续触碰叶片上的感觉毛两次时，叶片就会迅速闭合将猎物困住。',
    funFact: '捕蝇草不仅能消化昆虫，还能分辨出落入陷阱的是不是真正的食物（比如雨滴就不会让它闭合）。'
  },
  {
    id: 'k4',
    title: '帝企鹅 (Emperor Penguin)',
    category: 'animal',
    image: 'https://images.unsplash.com/photo-1598439210625-5067c578f3f6?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
    description: '帝企鹅是体型最大的企鹅，生活在严寒的南极洲。它们是唯一一种在南极洲的冬季进行繁殖的企鹅。',
    funFact: '企鹅爸爸会在零下40度的严寒中，把蛋放在脚面上孵化长达两个月！'
  }
];

interface KnowledgeQuizWidgetProps {
  currentCardId?: string;
  onCardChange?: (cardId: string) => void;
}

const KnowledgeQuizWidget: React.FC<KnowledgeQuizWidgetProps> = ({ currentCardId, onCardChange }) => {
  const { theme } = useTheme();
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (currentCardId) {
      const index = KNOWLEDGE_CARDS.findIndex(c => c.id === currentCardId);
      if (index !== -1) {
        setCurrentIndex(index);
      }
    }
  }, [currentCardId]);

  const handlePrev = () => {
    const newIndex = currentIndex === 0 ? KNOWLEDGE_CARDS.length - 1 : currentIndex - 1;
    setCurrentIndex(newIndex);
    onCardChange?.(KNOWLEDGE_CARDS[newIndex].id);
  };

  const handleNext = () => {
    const newIndex = currentIndex === KNOWLEDGE_CARDS.length - 1 ? 0 : currentIndex + 1;
    setCurrentIndex(newIndex);
    onCardChange?.(KNOWLEDGE_CARDS[newIndex].id);
  };

  const currentCard = KNOWLEDGE_CARDS[currentIndex];

  return (
    <motion.div 
      className={`relative flex flex-col w-[420px] h-[620px] ${theme.colors.cardBg} backdrop-blur-2xl rounded-[56px] shadow-[0_30px_60px_-15px_rgba(0,0,0,0.1),inset_0_0_0_1px_rgba(255,255,255,0.5)] border ${theme.colors.cardBorder} overflow-hidden transition-colors duration-500`}
      initial={{ scale: 0.9, opacity: 0, y: 20 }}
      animate={{ scale: 1, opacity: 1, y: 0 }}
      exit={{ scale: 0.9, opacity: 0, y: 20 }}
      transition={{ type: "spring", stiffness: 300, damping: 25 }}
    >
      {/* Image Header */}
      <div className="relative w-full h-64 shrink-0 overflow-hidden">
        <AnimatePresence mode="wait">
          <motion.img
            key={currentCard.id}
            src={currentCard.image}
            alt={currentCard.title}
            className="absolute inset-0 w-full h-full object-cover"
            initial={{ opacity: 0, scale: 1.1 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.9 }}
            transition={{ duration: 0.5 }}
            referrerPolicy="no-referrer"
          />
        </AnimatePresence>
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        
        {/* Category Badge */}
        <div className="absolute top-6 left-6 flex items-center gap-2 px-3 py-1.5 bg-white/20 backdrop-blur-md rounded-full border border-white/30 text-white shadow-sm">
          {currentCard.category === 'plant' ? <Leaf className="w-4 h-4" /> : <Bird className="w-4 h-4" />}
          <span className="text-xs font-bold tracking-widest uppercase">
            {currentCard.category === 'plant' ? '植物百科' : '动物世界'}
          </span>
        </div>
      </div>

      {/* Content Body */}
      <div className="flex-1 flex flex-col p-8 relative">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentCard.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.3 }}
            className="flex flex-col h-full"
          >
            <h2 className={`text-3xl font-black ${theme.colors.textPrimary} tracking-tight mb-4`}>
              {currentCard.title}
            </h2>
            <p className={`text-base font-medium ${theme.colors.textMuted} leading-relaxed mb-6 flex-1`}>
              {currentCard.description}
            </p>
            
            <div className={`p-4 rounded-2xl ${theme.colors.backgroundShapes} border ${theme.colors.cardBorder} shadow-inner`}>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xl">💡</span>
                <span className={`text-sm font-bold ${theme.colors.textPrimary} uppercase tracking-widest`}>你知道吗？</span>
              </div>
              <p className={`text-sm font-medium ${theme.colors.textMuted} leading-relaxed`}>
                {currentCard.funFact}
              </p>
            </div>
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Navigation Controls */}
      <div className="absolute top-1/2 -translate-y-1/2 left-0 right-0 flex justify-between px-4 pointer-events-none">
        <button 
          onClick={handlePrev}
          className="w-10 h-10 rounded-full bg-white/30 backdrop-blur-md border border-white/50 flex items-center justify-center text-white shadow-lg pointer-events-auto hover:bg-white/50 hover:scale-110 transition-all"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>
        <button 
          onClick={handleNext}
          className="w-10 h-10 rounded-full bg-white/30 backdrop-blur-md border border-white/50 flex items-center justify-center text-white shadow-lg pointer-events-auto hover:bg-white/50 hover:scale-110 transition-all"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>
    </motion.div>
  );
};

export default KnowledgeQuizWidget;
