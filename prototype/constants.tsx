
import React from 'react';
import { Cloud, Calendar, MessageCircle, Headphones, Gamepad2, Clock, Music, Heart, BookOpen, Palette, Star, Timer } from 'lucide-react';
import { ServiceCard } from './types';

export const ROBOT_MODES = {
  IDLE: 'Ready to play!',
  LISTENING: 'Listening...',
  THINKING: 'Thinking...',
  SPEAKING: 'Here we go!',
  HAPPY: 'Yay!',
  SLEEPING: 'Zzz...'
};

export const SERVICE_CARD_POOL: ServiceCard[] = [
  {
    id: 'card-timer',
    type: 'timer',
    title: '专注时钟',
    content: '番茄工作法助手',
    statusTip: '该专注一会了',
    icon: 'timer'
  },
  {
    id: 'card-story',
    type: 'podcast',
    title: '故事时间',
    content: '一起探索比特森林的奥秘',
    statusTip: '想听个故事吗？',
    icon: 'book'
  },
  {
    id: 'card-chat',
    type: 'chat',
    title: '随心聊天',
    content: '今天过得怎么样？',
    statusTip: '找我聊聊天吧',
    icon: 'chat'
  },
  {
    id: 'card-game',
    type: 'game',
    title: '益智小游戏',
    content: '寻找隐藏的星星',
    statusTip: '来玩个游戏？',
    icon: 'game'
  },
  {
    id: 'card-draw',
    type: 'game',
    title: '涂鸦创作',
    content: '画一架太空飞船',
    statusTip: '我们来画画吧',
    icon: 'palette'
  },
  {
    id: 'card-quiz',
    type: 'schedule',
    title: '趣味问答',
    content: '空间知识大挑战',
    statusTip: '考考你的知识',
    icon: 'star'
  }
];

export const getIcon = (type: string) => {
  switch (type) {
    case 'book': return <BookOpen className="w-6 h-6" />;
    case 'palette': return <Palette className="w-6 h-6" />;
    case 'star': return <Star className="w-6 h-6" />;
    case 'chat': return <MessageCircle className="w-6 h-6" />;
    case 'podcast': return <Headphones className="w-6 h-6" />;
    case 'game': return <Gamepad2 className="w-6 h-6" />;
    case 'alarm': return <Clock className="w-6 h-6" />;
    case 'music': return <Music className="w-6 h-6" />;
    case 'health': return <Heart className="w-6 h-6" />;
    case 'timer': return <Timer className="w-6 h-6" />;
    default: return <Star className="w-6 h-6" />;
  }
};
