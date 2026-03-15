
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
    id: 'card-quiz',
    type: 'quiz',
    title: '知识问答',
    content: '探索动植物的奥秘',
    statusTip: '来学点新知识吧',
    icon: 'star'
  },
  {
    id: 'card-podcast',
    type: 'podcast',
    title: '互动播客',
    content: '边听边聊的语音播客',
    statusTip: '想听个播客吗？',
    icon: 'podcast'
  },
  {
    id: 'card-chat',
    type: 'chat',
    title: '倾诉聊天',
    content: '有什么心事跟我说说吧',
    statusTip: '找我聊聊心事',
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
  }
];

export const PODCASTS = [
  {
    id: 'podcast-1',
    title: '比特森林的奇妙冒险',
    category: '故事',
    bgImage: 'https://picsum.photos/seed/forest/800/600',
    content: '在遥远的比特森林里，住着一只名叫艾瑟的小机器人。他每天都在森林里寻找丢失的代码片段。有一天，他遇到了一只会说话的电子猫头鹰。猫头鹰告诉他，在森林的深处，隐藏着一个可以解答任何问题的超级核心。于是，艾瑟踏上了寻找超级核心的冒险之旅。一路上，他遇到了各种各样的挑战，比如需要解开逻辑谜题才能通过的逻辑之桥，还有会干扰信号的电磁迷雾。但艾瑟凭借着他的智慧和勇气，一次次化险为夷。',
  },
  {
    id: 'podcast-2',
    title: '宇宙黑洞的奥秘',
    category: '知识',
    bgImage: 'https://picsum.photos/seed/blackhole/800/600',
    content: '黑洞是宇宙中最神秘的天体之一。它的引力极其强大，甚至连光都无法逃脱。今天我们来聊聊黑洞是如何形成的。当一颗质量极大的恒星耗尽了它的核燃料，它就会发生超新星爆发，核心部分会坍缩成一个密度极高的点，这就是奇点。在奇点周围，有一个被称为事件视界的边界，一旦越过这个边界，任何东西都无法逃脱黑洞的引力。如果你掉进黑洞会发生什么呢？科学家们认为，你会被强大的引力拉伸成一根面条，这个过程被称为“意大利面化”。',
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
