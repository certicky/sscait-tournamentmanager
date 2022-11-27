SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `sc`
--

--
-- Table structure for table `achievements`
--
CREATE TABLE IF NOT EXISTS `achievements` (
  `type` varchar(128) NOT NULL,
  `bot_id` int(11) NOT NULL,
  `datetime` int(11) NOT NULL,
  KEY `bot_id` (`bot_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `achievements`
--
INSERT INTO `achievements` (`type`, `bot_id`, `datetime`) VALUES
('winningStreak3', 1, 1360453967),
('winningStreak5', 1, 1360443573),
('experienced', 1, 1360408919),
('winningStreak3', 2, 1360408919),
('godlike', 2, 1360407305),
('veteran', 2, 1360407305),
('experienced', 2, 1360407305),
('winningStreak10', 2, 1360407305),
('winningStreak5', 3, 1360407305),
('winningStreak3', 3, 1360407305),
('veteran', 5, 1360406505),
('experienced', 5, 1360406505),
('experienced', 5, 1360402870),
('winningStreak5', 5, 1360402869);

-- --------------------------------------------------------

--
-- Table structure for table `achievement_texts`
--

CREATE TABLE IF NOT EXISTS `achievement_texts` (
  `ordering` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(128) NOT NULL,
  `title` varchar(128) NOT NULL,
  `text` varchar(256) NOT NULL,
  PRIMARY KEY (`ordering`),
  UNIQUE KEY `type` (`type`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=25 ;

--
-- Dumping data for table `achievement_texts`
--
INSERT INTO `achievement_texts` (`ordering`, `type`, `title`, `text`) VALUES
(1, 'equalOpportunity', 'Equal opportunity ass kicker', 'Win at least one game against all 3 races.'),
(2, 'winningStreak3', 'Winning Streak 3', 'Win 3 games in a row.'),
(3, 'winningStreak5', 'Winning Streak 5', 'Win 5 games in a row.'),
(4, 'winningStreak10', 'Winning Streak 10', 'Win 10 games in a row.'),
(5, 'experienced', 'Experienced', 'Have at least 50% win rate over 4 days.'),
(6, 'veteran', 'Veteran', 'Have at least 65% win rate over 4 days.'),
(7, 'godlike', 'Godlike', 'Have at least 85% win rate over 4 days.'),
(8, 'pieceOfCake', 'Piece of Cake', 'Win 100 games.'),
(9, 'letsRock', 'Let''s Rock', 'Win 500 games.'),
(10, 'comeGetSome', 'Come Get Some', 'Win 2000 games.'),
(11, 'damnImGood', 'Damn, I''m Good!', 'Win 5000 games.'),
(12, 'vsZerg50', 'vs Zerg 50', 'Win 50 games against Zerg opponents.'),
(13, 'vsZerg200', 'vs Zerg 200', 'Win 200 games against Zerg opponents.'),
(14, 'vsZerg500', 'vs Zerg 500', 'Win 500 games against Zerg opponents.'),
(15, 'vsTerran50', 'vs Terran 50', 'Win 50 games against Terran opponents.'),
(16, 'vsTerran200', 'vs Terran 200', 'Win 200 games against Terran opponents.'),
(17, 'vsTerran500', 'vs Terran 500', 'Win 500 games against Terran opponents.'),
(18, 'vsProtoss50', 'vs Protoss 50', 'Win 50 games against Protoss opponents.'),
(19, 'vsProtoss200', 'vs Protoss 200', 'Win 200 games against Protoss opponents.'),
(20, 'vsProtoss500', 'vs Protoss 500', 'Win 500 games against Protoss opponents.'),
(21, 'cheese', 'Cheese!', 'Win 3 games in less than 30 minutes.'),
(22, 'flowerChild', 'Flower Child', 'End 3 games in a draw in one day.'),
(23, 'legendary', 'Legendary', 'Unlock all the achievements.'),
(24, 'supporter', 'Supporter', 'Help the SSCAI tournament and BWAPI community by providing technical assistance or donating in fundraising campaigns.');

-- --------------------------------------------------------

--
-- Table structure for table `fos_user`
--
CREATE TABLE IF NOT EXISTS `fos_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8 NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8 NOT NULL,
  `temporary_password_hash` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `full_name` varchar(255) CHARACTER SET utf8 NOT NULL,
  `student` tinyint(1) NOT NULL DEFAULT '0',
  `school` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT 'N/A',
  `confirmation_token` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
  `email_confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `bot_path` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `bot_race` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `bot_description` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `bot_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `bot_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_update_time` datetime NOT NULL DEFAULT '2012-10-10 00:00:00',
  `custom_flags` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `confirmation_token` (`confirmation_token`),
  KEY `bot_path` (`bot_path`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=172 ;

--
-- Dumping data for table `fos_user` (test users have password set to 'test')
--

INSERT INTO `fos_user` (`id`, `email`, `password_hash`, `temporary_password_hash`, `full_name`, `student`, `school`, `confirmation_token`, `email_confirmed`, `bot_path`, `bot_race`, `bot_description`, `bot_enabled`, `bot_type`, `last_update_time`, `custom_flags`) VALUES
(1, 'xxx0@gmail.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User01', 1, 'UC Berkeley', NULL, 1, '/path/to/bot', 'Zerg', 'Hydra/Lurker bot.', 0, NULL, '2012-10-10 00:00:00', ''),
(2, 'xxx1@gmail.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User02', 1, 'Comenius University, Bratislava', NULL, 1, '/path/to/bot', 'Protoss', 'Simple Protoss bot.', 1, 'JAVA_JNI', '2012-10-10 00:00:00', ''),
(3, 'xxx2@gmail.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User03', 0, 'Czech Technical University in Prague', NULL, 1, '/path/to/bot', 'Protoss', 'XIMP bot. Carrier push.', 1, 'AI_MODULE', '2013-12-03 00:14:37', 'ctu_open;'),
(4, 'xxx3@yahoo.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User04', 0, 'N/A', NULL, 1, '/VM Shared/Bots/path/to/bot', 'Zerg', 'Priority state machine. Uses prediction and grid-based(mostly search) algorithms to evaluate best building placements, defence/attack positions, and enemy attack state.', 1, 'AI_MODULE', '2014-12-22 10:43:47', ''),
(5, 'xxx4@gmail.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User05', 1, 'Comenius University, Bratislava', NULL, 1, '/path/to/bot', 'Terran', 'Terran bot using priorities generated based on various input with army composition based on enemy''s.', 1, 'JAVA_JNI', '2012-10-10 00:00:00', ''),
(6, 'xxx5@gmail.com', '098f6bcd4621d373cade4e832627b4f6', '', 'Test User06', 1, 'Comenius University, Bratislava', 'dt2q9p0tesuhgxx4', 1, '/path/to/bot', 'Zerg', 'Simple Hydra bot.', 1, 'JAVA_JNI', '2012-10-10 00:00:00', '');

-- --------------------------------------------------------

--
-- Table structure for table `games`
--

CREATE TABLE IF NOT EXISTS `games` (
  `game_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bot1` int(11) NOT NULL,
  `bot2` int(11) NOT NULL,
  `datetime` int(11) NOT NULL,
  `result` enum('1','2','draw','unfinished','error') NOT NULL DEFAULT 'unfinished',
  `map` varchar(128) NOT NULL,
  `note` varchar(255) NOT NULL,
  `bot1score` int(11) NOT NULL,
  `bot2score` int(11) NOT NULL,
  PRIMARY KEY (`game_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=862 ;

--
-- Dumping data for table `games`
--

INSERT INTO `games` (`game_id`, `bot1`, `bot2`, `datetime`, `result`, `map`, `note`, `bot1score`, `bot2score`) VALUES
(1, 1, 2, 1421168656, '1', 'maps/sscai/(3)TauCross.scx', '', 12006, 4946),
(2, 3, 4, 1421169852, '1', 'maps/sscai/(3)TauCross.scx', 'bot2_crashed;', 0, -100),
(3, 5, 6, 1421170441, '2', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 16474, 48783),
(7, 4, 2, 1421172868, '1', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 16218, -100),
(12, 2, 1, 1421175044, '2', 'maps/sscai/(4)CircuitBreaker.scx', '', 11294, 40516),
(14, 4, 1, 1421175589, '1', 'maps/sscai/(4)ElectricCircuit.scx', '', 12262, 4417),
(20, 3, 4, 1421178803, '1', 'maps/sscai/(3)TauCross.scx', '', 145359, 83411),
(24, 6, 3, 1421180496, '1', 'maps/sscai/(4)Jade.scx', '', 89755, 23960),
(29, 4, 2, 1421182823, '1', 'maps/sscai/(4)EmpireoftheSun.scm', '', 157411, 86601),
(32, 6, 5, 1421184000, '2', 'maps/sscai/(4)FightingSpirit.scx', '', 7987, 23260),
(33, 4, 6, 1421184866, '2', 'maps/sscai/(4)Icarus.scm', '', 80957, 152574),
(34, 5, 1, 14211856, '1', 'maps/sscai/(4)CircuitBreaker.scx', '', 13564, 2479),
(35, 1, 2, 1421185749, '1', 'maps/sscai/(2)Destination.scx', 'bot2_crashed;', 59112, -100),
(36, 5, 4, 1421186274, '2', 'maps/sscai/(4)CircuitBreaker.scx', '', 16089, 58539),
(41, 5, 6, 1421188204, '1', 'maps/sscai/(4)CircuitBreaker.scx', '', 0, 2934),
(42, 5, 3, 1421189521, '2', 'maps/sscai/(3)NeoMoonGlaive.scx', 'draw;', 46789, 53518),
(46, 6, 3, 14211914, '1', 'maps/sscai/(4)Icarus.scm', '', 140430, 34682),
(51, 3, 4, 14211924, '2', 'maps/sscai/(3)NeoMoonGlaive.scx', 'bot1_crashed;', -100, 0),
(54, 2, 6, 1421193818, '1', 'maps/sscai/(4)LaMancha1.1.scx', '', 30552, 8055),
(55, 6, 2, 14211941, '2', 'maps/sscai/(4)CircuitBreaker.scx', '', 11681, 23557),
(56, 1, 5, 1421194355, '2', 'maps/sscai/(4)Andromeda.scx', 'bot1_crashed;', -100, 5263),
(57, 5, 2, 1421194454, '2', 'maps/sscai/(4)ElectricCircuit.scx', '', -100, 406),
(58, 2, 3, 1421194730, '1', 'maps/sscai/(4)EmpireoftheSun.scm', '', 10328, 5469),
(59, 3, 6, 1421195075, '2', 'maps/sscai/(4)CircuitBreaker.scx', '', 9615, 24610),
(60, 3, 2, 1421195645, '2', 'maps/sscai/(4)LaMancha1.1.scx', '', 44872, 89961),
(61, 5, 1, 1421195842, '1', 'maps/sscai/(2)HeartbreakRidge.scx', '', 3652, 1237),
(62, 1, 6, 1421196215, '2', 'maps/sscai/(2)Destination.scx', '', 9355, 24185),
(63, 1, 4, 1421196587, '2', 'maps/sscai/(4)ElectricCircuit.scx', '', 3721, 16140),
(64, 4, 1, 14211973, '1', 'maps/sscai/(2)HeartbreakRidge.scx', '', 65029, 21997),
(65, 2, 1, 1421198642, '2', 'maps/sscai/(2)Benzene.scx', 'draw;', 61338, 63614),
(66, 4, 1, 1421199022, '1', 'maps/sscai/(4)FightingSpirit.scx', '', 17432, 3676),
(67, 2, 1, 1421199564, '1', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 38635, 16792),
(68, 4, 2, 1421200174, '2', 'maps/sscai/(2)HeartbreakRidge.scx', '', -100, 63403),
(69, 2, 6, 1421201241, '2', 'maps/sscai/(2)Benzene.scx', 'draw;', 4032, 4196),
(70, 3, 1, 1421201444, '2', 'maps/sscai/(3)TauCross.scx', '', 450, 6485),
(71, 1, 4, 1421201779, '1', 'maps/sscai/(4)EmpireoftheSun.scm', '', 13585, 4649),
(72, 4, 5, 1421202018, '1', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 8620, 3462),
(73, 2, 1, 1421202396, '2', 'maps/sscai/(2)HeartbreakRidge.scx', '', 9056, 25139),
(74, 3, 5, 1421202683, '2', 'maps/sscai/(4)Icarus.scm', '', 5173, 15262),
(75, 6, 5, 1421202859, '2', 'maps/sscai/(4)Jade.scx', '', 2100, 7792),
(76, 3, 4, 1421203329, '1', 'maps/sscai/(4)FightingSpirit.scx', '', 23766, 8599),
(77, 4, 5, 1421203422, '1', 'maps/sscai/(2)Benzene.scx', '', 450, -100),
(78, 4, 2, 1421203874, '2', 'maps/sscai/(4)ElectricCircuit.scx', '', 15017, 70778),
(79, 3, 3, 1421204287, '2', 'maps/sscai/(4)CircuitBreaker.scx', 'bot1_crashed;', -100, 21200),
(80, 5, 2, 1421204385, '2', 'maps/sscai/(2)Destination.scx', '', -100, 458),
(81, 4, 4, 1421204983, '1', 'maps/sscai/(4)EmpireoftheSun.scm', '', 51625, 15375),
(82, 3, 2, 1421205180, '1', 'maps/sscai/(4)LaMancha1.1.scx', '', 6033, 2495),
(83, 3, 5, 1421205649, '1', 'maps/sscai/(4)FightingSpirit.scx', '', 44337, 11130),
(84, 1, 4, 1421206299, '1', 'maps/sscai/(4)Icarus.scm', '', 81892, 23222),
(85, 3, 2, 1421206651, '2', 'maps/sscai/(4)Roadrunner.scx', '', 14580, 29795),
(86, 2, 2, 1421207070, '2', 'maps/sscai/(2)Destination.scx', '', 8414, 20364),
(87, 1, 1, 1421207300, '1', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 74, 3730),
(88, 5, 1, 1421207556, '2', 'maps/sscai/(4)Andromeda.scx', '', 3738, 15134),
(89, 5, 5, 1421207820, '2', 'maps/sscai/(4)FightingSpirit.scx', '', 4232, 11610),
(90, 3, 3, 1421208484, '2', 'maps/sscai/(4)LaMancha1.1.scx', '', 40927, 62294),
(91, 2, 5, 1421208935, '1', 'maps/sscai/(4)LaMancha1.1.scx', 'bot2_crashed;', 12861, -100),
(92, 2, 3, 1421209483, '2', 'maps/sscai/(2)Destination.scx', '', 17189, 62036),
(93, 5, 5, 1421209768, '1', 'maps/sscai/(4)Python.scx', '', 184, 4790),
(94, 2, 4, 1421212644, '2', 'maps/sscai/(4)LaMancha1.1.scx', '', 17813, 33231),
(95, 4, 1, 1421212962, '2', 'maps/sscai/(2)HeartbreakRidge.scx', '', 7434, 19381),
(96, 3, 3, 1421214231, '1', 'maps/sscai/(4)ElectricCircuit.scx', 'draw;', 46185, 29456),
(97, 2, 2, 1421214441, '2', 'maps/sscai/(4)Python.scx', '', 2517, 8879),
(98, 5, 5, 1421215664, '1', 'maps/sscai/(4)Roadrunner.scx', 'draw;', 54334, 42667),
(99, 3, 5, 1421215906, '1', 'maps/sscai/(2)HeartbreakRidge.scx', '', 7189, 4046),
(100, 2, 4, 1421216066, '1', 'maps/sscai/(4)Andromeda.scx', '', 4883, 2161),
(101, 5, 2, 1421217243, '1', 'maps/sscai/(4)Andromeda.scx', 'draw;', 41032, 22368),
(102, 4, 4, 1421217516, '1', 'maps/sscai/(4)Icarus.scm', '', 10012, 3460),
(103, 1, 2, 1421217819, '2', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 9349, 21755),
(104, 5, 3, 1421218357, '2', 'maps/sscai/(4)Python.scx', '', 36736, 72515),
(105, 2, 2, 1421218645, '2', 'maps/sscai/(2)HeartbreakRidge.scx', '', 6335, 14453),
(106, 6, 5, 1421218875, '1', 'maps/sscai/(4)Icarus.scm', '', 8210, 2989),
(107, 5, 2, 14212192, '2', 'maps/sscai/(4)Andromeda.scx', '', 4468, 15073),
(108, 5, 4, 1421219483, '1', 'maps/sscai/(4)FightingSpirit.scx', '', 23314, 7149),
(109, 5, 3, 1421219744, '1', 'maps/sscai/(4)Andromeda.scx', '', 7591, 4504),
(170, 1, 2, 0, 'unfinished', 'maps/sscai/(2)Benzene.scx', '', 0, 0),
(171, 4, 6, 0, 'unfinished', 'maps/sscai/(2)Benzene.scx', '', 0, 0),
(172, 2, 5, 0, 'unfinished', 'maps/sscai/(4)Icarus.scm', '', 0, 0),
(173, 2, 3, 0, 'unfinished', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 0, 0),
(174, 6, 3, 0, 'unfinished', 'maps/sscai/(2)Destination.scx', '', 0, 0),
(175, 5, 3, 0, 'unfinished', 'maps/sscai/(4)ElectricCircuit.scx', '', 0, 0),
(176, 2, 1, 0, 'unfinished', 'maps/sscai/(2)Benzene.scx', '', 0, 0),
(177, 4, 2, 0, 'unfinished', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 0, 0),
(178, 4, 2, 0, 'unfinished', 'maps/sscai/(3)NeoMoonGlaive.scx', '', 0, 0),
(179, 2, 1, 0, 'unfinished', 'maps/sscai/(4)FightingSpirit.scx', '', 0, 0);


/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


DROP TABLE IF EXISTS votes_for_players;

CREATE TABLE IF NOT EXISTS votes_for_players (
    game_id           BIGINT(20)  NOT NULL,
    bot_id            INT(11)     NOT NULL,
    user_ip           VARCHAR(39) NOT NULL,

    created_time_stamp TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (game_id, bot_id, user_ip),
    KEY INDEX_game_id (game_id),
    KEY INDEX_user_ip (user_ip)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;


DROP TABLE IF EXISTS all_time_votes_for_players;

CREATE TABLE IF NOT EXISTS all_time_votes_for_players (
    bot_id            INT(11)     NOT NULL,

    total_votes INT NOT NULL DEFAULT 0,

    PRIMARY KEY (bot_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS historical_elo_ratings;

CREATE TABLE IF NOT EXISTS historical_elo_ratings (
    bot_id INT(11) NOT NULL,
    date date NOT NULL,
    elo_rating INT NOT NULL,

    PRIMARY KEY (bot_id, date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
