# Tarombo
### _Create your own family tree_

Tarombo is an app for Android designed to manage family trees.
At the moment it's completely free and open source.
It is a fork of Family Gem.

## Features
With Tarombo you can:
- Create a family tree from scratch, entering names, dates, places, various events, photos and sources.
- Import an existing family tree through a GEDCOM file and modify it as you want.
- Export the family tree you created (via GEDCOM again) to import in every other genealogy program.
- Share a tree with your relatives, letting them improve it and receiving back the updates. Then you can choose whether accept them or not.

Minimum supported version is Android 4.4 KitKat (API 19), maximum is Android 12 S (API 31).
The intent is that data structure respects as much as possible the latest version of GEDCOM standard: [5.5.1](https://www.familysearch.org/developers/docs/gedcom/) and possibly also [5.5.5](https://www.gedcom.org/gedcom.html).<br>
Family Gem is strongly based on the library [Gedcom 5 Java](https://github.com/FamilySearch/gedcom5-java) by FamilySearch.

## Limitations
The code provided in this repository should compile and build a working version of Tarombo, but with some limitations:
|Missing|Limitation|
|-|-|
|App signature|You loose saved trees when you install over a signed version|
|Server account|You can't share trees|
|GeoNames "demo" account|Place names suggestions probably don't appear|

The code (classes, variables, comments...) is almost all written in Italian (my native language of Family Gem's developer).

## Translation
The translation of Tarombo user interface is managed on [Weblate](https://hosted.weblate.org/projects/tarombo/app/).
Contribution to translation in any language is really appreciated.
You need a Weblate account to freely work there, but also without account you can make suggestions to already existing translations.

[![Translation status](https://hosted.weblate.org/widgets/tarombo/-/multi-auto.svg)](https://hosted.weblate.org/engage/tarombo/)

## Resources
Official website: tarombo.siboro.org

You can find Tarombo on [Google Play](https://play.google.com/store/apps/details?id=org.siboro.tarombo).

## Feedback
For questions, bugs, suggestions you can:
- [Open an issue](https://github.com/tarombo/FamilyGem/issues)
- Start a conversation on the [Family Gem Google group](https://groups.google.com/g/tarombo-app) (Google account required)
- Directly email to the Family Gem Google group: tarombo-app@googlegroups.com (no account required)

## License
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

## About
Tarombo is written in Java within Eclipse and Android Studio.
Project started on beginning of 2018.
Author is Arnold Siboro and Putrasto (Family Gem's author is Michele Salvador).
