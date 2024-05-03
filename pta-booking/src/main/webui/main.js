/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import theaterJS from 'theaterjs';

const theater = theaterJS();

theater
    .on('type:start, erase:start', function() { theater.getCurrentActor().$element.classList.add('actor-content--typing');})
    .on('type:end, erase:end', function() {theater.getCurrentActor().$element.classList.remove('actor-content--typing');});

theater
    .addActor('Quarkus', { speed: 1, accuracy: 0.7 })
    .addActor('Me', { speed: 0.9, accuracy: 0.8 })
    .addScene('Quarkus:Toc toc.', 1000)
    .addScene('Me:What?', 500)
    .addScene('Quarkus:You will eat Quinoa today!', 200)
    .addScene('Me:Nooo...', -3, '!!! ', 150, 'No! ', 150)
    .addScene('Me:Yuk! That\'s impossible!', 100)
    .addScene('Quarkus:It is time!', 100)
    .addScene('Quarkus:With your training and this power,', 100)
    .addScene('Quarkus:You will create awesome web apps.', 100)
    .addScene('Quarkus:It is your destiny!', 200)
    .addScene('Quarkus:Meet Quarkus UI with NO hAssle!', 200)
    .addScene('Me:Neat!', 200)
