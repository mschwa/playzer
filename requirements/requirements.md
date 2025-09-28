# Application User Interface (Describe the overall style and interface commonalities)

## Main Screen

### Main Screen Header Panel

#### Main Screen Header Panel First Row

Left aligned on this first row is a hamburger menu icon. Its height is 1/12 of the overall screen size. Pressing this icon opens a small panel on the left side with options for “Settings” and “Equalizer”. Pressing each option opens its respective screen (described below) in a swipe left fashion. To the right of the hamburger menu is a “My Music” label. Right aligned on this row is a search icon (magnifying glass). When pressed, the interface opens the Search Screen in a swipe left fashion.

#### Main Screen Header Panel Second Row

Left aligned on this second row is a minimized music player. Its height is 1/12 of the overall screen size. Left aligned on this minimized music player is the album cover of the currently selected song cropped in a small circle. To the right of the album cover is a label of the title of the song, with a smaller label of the artist name of the song right underneath. To the right of the title/artist label is a press play/pause icon which controls the current track.

### Main Screen Main Panel

#### Main Screen Main Panel Tab Select Row

The tab select row contains tab headers: “Tracks”, “Playlists”, “Albums”, and “Artists”. Its height is 1/24 of the overall screen size. Pressing any of these headers opens the respective listing in the listing panel.

#### Main Screen Main Listing Panel (Describe the style and common interface functionality here)

The main listing panel contains four different listing tabs: “Tracks”, “Playlists”, “Albums”, and “Artists”. The default tab is the “Tracks” tab. The listing panel takes up the rest of the screen below the header panel and tab select row. The listing view before scrolling holds a little over 9 rows, where each row takes up about 1/12 of the overall screen size.

##### Tracks Listing Tab

###### *Tracks Listing Tab First Row*

The first row of the tracks tab has a count of the total tracks on the left hand side and a sort by icon on the right hand side. Pressing on the sort icon opens up a small pop up window that has the option to sort by “Title”, “Date Added”, “Album”, “Artist”, “Ascending”, and “Descending” and re-loads the Track Listing Common Control (see below) accordingly. Its height is 1/24 of the overall screen size.

###### *Tracks Listing Tab Track Listing Panel*

This track list panel loads the Track Listing Common Control (see User Interface Commonalities) with all tracks loaded.

On the lower right-hand side of the track listing, there is a hovering shuffle icon. Pressing this icon shuffles the tracks and loads them into the Music Player in random order.

##### Playlist Listing Tab

Each playlist row begins left to right with the same playlist icon. To the right of the playlist icon is the name of the playlist. Right aligned on each track row there’s a 3 dot vertical ellipsis icon which when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the playlist. The options are “Play”, “Edit Playlist Name” and “Delete Playlist”. 

###### *Playlist Listing Item Options Functionality* 

* Pressing the “Play” option opens up the Music Player with the entire playlist queued.   
* Pressing on the “Edit Playlist Name” options opens up a small, centered prompt window labeled “Edit Playlist Name”. This window has a text field for the playlist name and two small text buttons for “CANCEL” and “OK”, which respectively close the window and update the playlist name. When the “OK” button is pressed, and after the playlist name is updated, a small toast message appears at the bottom of the panel which reads “Playlist Renamed”. The playlist listing tab also is updated with the new playlist name.   
* Pressing the “Delete Playlist” option also opens up a small, centered prompt window. This window is labeled “Confirm Delete”, with a smaller label underneath reading “Are you sure you want to permanently delete this Playlist?”. This window has two small text buttons for “Yes” and “No”. Pressing “Yes” deletes the Playlist, clears the windows and presents a toast message on the listing panel which reads “Playlist Deleted”. Pressing “No” just clears the window.

On the lower right hand side of the playlist listing there’s a hovering plus sign icon. Pressing this opens up a small, centered prompt window labeled “Create New Playlist”. This window has a text field for the playlist name and two small text buttons for “CANCEL” and “OK”, which respectively close the window and create the new playlist.

Pressing on a Playlist row opens up the Playlist Screen in a swipe left fashion.

##### Albums Listing Tab

Left aligned on the first row of the albums listing tab is a label with the count of the total number of albums, and then a space, and then the text “Albums”. Right aligned is a sort by icon. Pressing on the sort icon opens up a small pop up window that gives me the option to sort by “Ascending” and “Descending”. The default sort is Ascending.

Under the first row is the main album listing. Left aligned on each album row is the album cover image cropped in a small circle. To the left of this image is a label of the album title with another label of the artist name underneath it in smaller text. Right aligned on each track row there’s a 3 dot vertical ellipsis icon which when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the track. The options are “Play”, “Add to Playlist” and “Delete”.

###### Album *Listing Item Options Functionality*

* Pressing the “Play” option opens up the Music Player with all of the tracks from the artist queued.   
* Pressing “Add to Playlist” opens up the Add to Playlist Screen with all of the tracks from the artist selected.   
* Pressing the “Delete” option opens the Delete Audio Files Common Control (see User Interface Commonalities) for the album.

Pressing on an album row opens up the Album Screen in a swipe left fashion.

##### Artists Listing Tab

Left aligned on the first row of the artists listing tab is a label with the count of the total number of artists and then a space and then the text “Artists”. Right aligned is a sort by icon. Pressing on the sort icon opens up a small pop up window that gives me the option to sort by “Ascending” and “Descending”. The default sort is Ascending.

Under the first row is the main artist listing. Left aligned on each artist row is a label with the artist name. Right underneath this label is another label with the number of albums for the artist, and then a space, and then the text “Albums”, all in smaller text. Right aligned on each artist row is another label with the total number of tracks from the artist, and then a space, and then the text “Tracks”. To the right of this, there’s a 3 dot vertical ellipsis icon which when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the artist. The options are “Play”, “Add to Playlist”, and “Delete”.

###### Artist *Listing Item Options Functionality*

* Pressing the “Play” option opens up the Music Player with the tracks of the album queued.  
* Pressing “Add to Playlist” opens up the Add to Playlist Screen with all of the tracks in the album selected.   
* Pressing the “Delete” option opens the Delete Audio Files Common Control (see User Interface Commonalities) for the artist.

Pressing on an artist row opens up the Artist Screen in a swipe left fashion.

## Music Player Screen

### Music Player Screen Top Row:

Left aligned on the top row of this screen is a back arrow. Pressing on the arrow returns the user to the previous screen in a swipe right fashion. Right aligned is an equalizer icon and then to the right of that is a 3 dot vertical ellipsis icon. When the ellipsis icon is pressed it opens up the options of the track. The options are “Add to Playlist”, “Go to Album” and “Go to Artist”.

#### Track Options

* Pressing “Add to Playlist” opens up the Add to Playlist Screen with the current track selected.  
* Pressing “Go to Album” opens up the Album Screen of the track in the Music Player.  
*  Pressing “Go to Artist” opens up the Artist Screen of the track in the Music Player.

This row is about 1/16 the height of the entire screen.

### Music Player Screen Track Image Row:

The next row contains/displays the track’s album cover image. This row/image is about ⅓ the height of the entire screen. A default image is displayed if the album cover file cannot be found.

### Music Player Screen Track Control Row:

Left aligned on the control row is an equalizer Icon. Pressing this icon brings up the Equalizer Screen in swipe left fashion. Centered in this row are the previous track arrows, the play/pause pause icon, and the next track arrows. Pressing the previous track arrows loads the previous track from the internal queue into the music player. Pressing the play icon plays the current track and swaps the icon with the play icon pause icon. Pressing the pause icon pauses the current track and swaps the icon with the pause icon with the play icon. Pressing the next track arrows loads the next track from the internal queue into the music player. Right aligned on the control row is a volume icon. Pressing the volume icon opens up the device’s volume slider.

### Music Player Screen Track “Seekbar” (or seek slider) Row:

Left aligned on the seekbar row is a label with the current time in track in MM:SS format. This label is updated every second that the track is playing. Centered is the Seekbar. A horizontal line with a small red dot that travels the line with the passing seconds of the track. The dot can be dragged and dropped to any second of the track. Right aligned is a label of the track total time in MM:SS format.

## Equalizer Screen

### Equalizer Screen Top Row: 

Left aligned on the top row of this screen is a back arrow. Following left to right is a label of “Equalizer”. Right aligned is a toggle switch which turns the Equalizer effects on and off.

### Equalizer Screen Bass Boost Row: 

Left aligned on this row is a label titled “Bass Boost”. Underneath this label is a horizontal slider which changes the value of the bass boost.

### Equalizer Screen Virtualizer Row:

Left aligned on this row is a label titled “Virtualizer”. Underneath this label is a horizontal slider which changes the value of the virtualizer. 

### Equalizer Screen Presets Control:

Left aligned on this control is a label titled “Presets”. Underneath the presets label is an equalizer control with 5 vertical sliders. The first slider controls the 30 Hz through 120 Hz frequency. The second slider controls 120 Hz through 460 Hz. The third slider controls the 460 Hz through 1000 Hz. The 4th slider controls 1,000 Hz through  7,000 Hz. The 5th and final slider controls the 7,000 Hz through 20,000 Hz. Each slider has a label underneath with their respective Hz control ranges.

Underneath these sliders is a horizontal button that spans all five sliders and is labeled “Flat”. Clicking this button returns each slider setting to the respective average or middle value.

## Playlist Screen

### Playlist Screen Header Panel

The header panel has a background image which is a randomly selected album cover from one of the tracks in the playlist. Once the image is selected it stays static as the image for the playlist. The panel is roughly 1/4 the size of the entire screen. The header panel minimizes to just the header panel top row on scroll down of the listing panel. It maximizes back to the full header panel once scrolling up reaches the beginning of the track listing.

#### Playlist Screen Header Panel Top Row

The header panel top row contains the Sort and Search Header Panel Row Common Control.

#### Playlist Screen Header Panel Bottom Row

Left aligned on the header panel bottom row is the title of the playlist in a medium sized font. Below this label in a small sized font is the total number of tracks, followed by the total time of the playlist.

### Playlist Screen Track Listing Panel

Each row of the track listing begins left aligned with a hamburger menu looking icon with four lines. Pressing on this icon allows the user to drag and drop the order of the tracks in the playlist. To the right of this icon is a label with the track title with the artist name labeled below it.   
Right aligned on each track row there’s a 3 dot vertical ellipsis icon which when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the track. The options are “Play”, “Add to Playlist”, and “Remove from Playlist”.

#### Playlist Options Functionality

* Pressing the “Play” option opens up the Music Player with the track queued. Pressing “Add to Playlist” opens up the Add to Playlist Screen with the current track selected. Pressing “Remove from Playlist” functionally removes the track from the row from the playlist and displays a toast message at the bottom of the screen which is labeled “Removed x from the Playlist” (where x is the name of the track from the row).

Pressing on the space between the drag and drop icon and the track options on a row opens up the Music Player Screen with the track from the row loaded as the current track.

On the lower right hand side of the track listing there’s a hovering shuffle icon. Pressing this icon loads all of the tracks from the Playlist into the Music Player Screen in a randomly selected order.

## Add to PlayList Screen

### Add to PlayList Screen Top Row: 

Left aligned on the top row is the label “Select Playlist”.

### Add to PlayList Screen Second Row: 

This row contains a horizontal button that spans the width of the screen and is labeled “Create New Playlist”. Pressing this opens up a small, centered prompt window labeled “Create New Playlist”. This prompt window has a text field for the playlist name and two small text buttons for “CANCEL” and “OK”. Pressing “CANCEL” closes the prompt window. Pressing “OK” functionality creates the new playlist and displays a toast message at the bottom of the screen labeled “Playlist Created”. 

### Add to PlayList Screen Playlist Listing Panel: 

Each playlist row begins left to right with the same playlist icon. To the right of the playlist icon is the name of the playlist. Pressing on a playlist row functionally adds the selected track(s) to the playlist and displays a toast message labeled “Added to x” (x being the name of the selected playlist) while sending the UI back to the previous screen.

## Artist Screen

### Artist Screen Header Panel

The header panel has a background image which is a randomly selected album cover from one of the tracks in the playlist. It is roughly 1/4 the size of the entire screen.

#### Artist Screen Header Panel Top Row

The header panel top row contains the Sort and Search Header Panel Row Common Control.

#### Artist Screen Header Panel Bottom Row

Left aligned on this panel bottom row is a horizontal sliding display of all the artist's album covers.

### Artist Screen Track List Panel

This track list panel loads the Track Listing Common Control (see User Interface Commonalities) with the tracks from the artist.

## Album Screen

### Album Screen Header Panel

The header panel has a background image which is the album cover (cropped to the pane size) from one of the tracks in the playlist. The panel size is roughly 1/4 the size of the entire screen.

#### Album Screen Header Panel Top Row

The header panel top row contains the Sort and Search Header Panel Row Common Control.

#### Album Screen Header Panel Bottom Row

Left aligned on this panel bottom row is the album titles label. Underneath in smaller text is the number of tracks, followed by the label “track(s)”.

### Album Screen Track List Panel

This track list panel loads the Track Listing Common Control (see User Interface Commonalities) with the tracks from the album.

## Search Screen

### Search Screen Header Row

Left aligned on the top row of this screen is a back arrow. Pressing on the arrow returns the user to the previous screen in a swipe right fashion.The main search text field centers this row. When there has been no text entered into this field it contains a greyed out label titled “Search Music Library…”. Right aligned in this row is an “x” icon. When pressed it clears any text input out of the search field

### Search Screen Results Listing Panel

The results listing panel has three collapsable result sections with red title rows.The 3 result  sections are “Tracks”, “Albums” and “Artists”. Left aligned on each result  section title row is the section name, followed by open parentheses, and then the number of results in the section, and then close parentheses. Right aligned on each result section title row is a down arrow icon. Pressing this icon collapses or expands the section.

The listing panel for each result section is auto populated by type ahead search in the main search text field (Search Screen Header Row).

### Tracks Search Results Listing Panel

The tracks search results panel loads the Track Listing Common Control (see User Interface Commonalities) with the tracks returned from the search.

#### Options Functionality

### Albums Search Results Listing Panel

Left aligned on each album search result row is the album title label with artist name label in smaller text underneath. Right aligned is a number of tracks label (number of tracks in the album) time in smaller, grey text, followed by the 3 dot vertical ellipsis icon which when when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the album.The options are “Play”, “Add to Playlist” and “Delete”.

#### Options Functionality

* Pressing the “Play” option opens up the Music Player with the tracks of the album queued.  
* Pressing “Add to Playlist” opens up the Add to Playlist Screen with all of the tracks in the album selected.   
* Pressing the “Delete” opens up the Delete Audio Files Common Control (see User Interface Commonalities) for the album.

### Artists Search Results Listing Panel

Left aligned on each artist search result row is the artist name label with a number of albums label (number of albums from the artist) in smaller grey text underneath. Right aligned is a number of tracks label (number of tracks in the album) time in smaller, grey text, followed by the 3 dot vertical ellipsis icon which when pressed opens up the Options Panel Common Control (see User Interface Commonalities) for the album.The options are “Play”, “Add to Playlist” and “Delete”.

#### Options Functionality

* Pressing the “Play” option opens up the Music Player with the tracks of the artist queued.  
* Pressing “Add to Playlist” opens up the Add to Playlist Screen with all of the tracks in the album selected.   
* Pressing the “Delete” option opens the Delete Audio Files Common Control (see User Interface Commonalities) for the artist.

## Settings Screen

### Settings Screen Top Row

Left aligned on the top row of this screen is a back arrow. Pressing on the arrow returns the user to the previous screen in a swipe right fashion.Followed to the right of the back arrow is a label titled “Settings”

### Settings Screen Main Panel

#### Root Folder Setting Row

#### Rescan Music Library Setting Row

#### Recently Added Playlist Track Count Setting Row

#### Backup Playlists Now Setting Row

#### Periodically Backup Playlists On/Off Setting Row

## User Interface Styling 

General Background Color: Charcoal  
Header Rows Font Color: White  
Listing Rows Font Color: Very light grey  
General Font: Arial  
Listing (any listing related panel in the app) Alternating Row Color: Grey

## User Interface Commonalities

### Track Listing Common Control

Left aligned on each track search result row is the track title label with artist name label in smaller text underneath. Right aligned is a track time label in smaller, grey text, followed by the 3 dot vertical ellipsis icon which, when clicked, opens the options for the track. The options are “Play”, “Add to Playlist”, and “Delete”.

###### *Track Listing Item Options Functionality*

* Pressing the “Play” option opens up the Music Player with the track queued.   
* Pressing “Add to Playlist” opens up the Add to Playlist Screen with the current track selected.  
* Pressing the “Delete” option opens the Delete Audio Files Common Control (see below) for the track.

Pressing on a track row anywhere to the right of the 3 dot vertical ellipsis icon opens up the Music Player Screen with the track from the row loaded as the current track.

The screen which loads the track listing control will inform it on which tracks to load. The Main Screen loads all tracks, the Search Screen loads tracks from the search results, the Artist Screen and the Album Screen loads tracks from the artist.

### Sort and Search Header Panel Row Common Control

The Playlist, Album, and Artist Screens share common user functionality in their respective header panel tops rows. Left aligned on the top row of this screen is a back arrow. Pressing on the arrow returns the user to the previous screen in a swipe right fashion. To the right of the back arrow is an optional label. The only screen which displays text in this optional label is the artist screen which displays the name of the selected artist. Right aligned on this row is the sort icon, and then the search icon at the far right. Pressing on the sort icon opens up a small pop up window that gives me the option to sort the tracks in the track listing by track name Ascending or Descending. Pressing the Search Icon opens up the Search Screen in swipe left fashion.

### Options Panel Common Control

An options panel is just a small modal window which opens up right to left from the right side of the screen where its corresponding 3 dot vertical ellipsis is located. The background color of the window is white and the text is black. The corresponding screen which loads the option describes the functionality for each option in its corresponding screen requirements section from above.

### Delete Audio Files Common Control

The Delete Audio Files Common Control is a medium sized black modal window which opens up in the middle of the screen. The window has a left aligned, bold white label which varies depending on its corresponding listing row type as such…

* Track listing row: “Allow this App to Permanently Delete this Audio File?”.  
* Artist listing row: “Allow this App to Permanently Delete all x Number of Audio Files for this Artist?” (where x is the total number of tracks from the artist).  
* Album listing row: “Allow this App to Permanently Delete all x Number of Audio Files from this Album?” (where x is the total number of tracks from the artist).

If the row is a Track, or Album listing, underneath the label is a small image of the album cover. If the row is an Artist listing, small images from the first three albums of the artist are displayed (center aligned) below the label.

Underneath the album cover(s) and right aligned are two two small text buttons for “Allow” and “Deny”. Pressing “Allow” deletes the music file(s) from the device, clears the prompt window and presents a toast message at the bottom of the listing screen labeled “Track(s) Permanently Deleted”. Pressing “Deny” clears the modal window.

### List Scrolling

All lists are scrolled by finger directly on the corresponding list display panel. List scrolling is fast and smooth. A very thin vertical line appears on the far left of the screen while scrolling with a small, thin, red rounded rectangle indicator for the position in the list which slides up and down with the scrolling motion. The list continues scrolling in the direction of the finger movement once the finger is removed, slowing it’s pace for three seconds before halting the scroll.

# Application Internal Components

## Internal Queue

The internal queue is a temporary, dynamic list of track pointers which dictates the playback order for the device music player API. While a playlist is a persisted, static collection, the queue is a "working area" for the current listening session. It holds the tracks to be played next and manages how they are added, removed, and ordered. The list is populated by the user interface selection of track, album, artist or playlist. The order of the list by default is the order of the input, however, can be shuffled (randomly reordered) at time of queuing.

#### Internal Data Structure

The queue should be implemented using a data structure like a doubly linked list, allowing for efficient insertion, deletion, and reordering of songs. A doubly linked list is well-suited because each node (track) contains pointers to both the previous and next track in the sequence, enabling seamless navigation for features like "back" and "next".

#### Key Functionality and Purpose

* Temporary flexibility: The internal queue provides a flexible, temporary data structure for the application to arrange music playing sessions on the fly without changing the users saved playlists.  
* User control: It allows for fine-grained control over playback order, including adding and removing individual tracks, playlists, entire albums or artists tracks.  
* Session-based listening: The queue is transient and cleared when the application closes or a new track, playlist, album or artist is selected for load into the music player.

## Track Cache

The track cache is a temporary data structure which holds pointers (file locations) and meta data for the audio files on the device. It allows for fast reading of track meta data and optimized file retrieval for the device's internal music player. It’s essentially a database of pointers so that the application doesn’t have to search from the file system of the device for essential functionality.

The track cache runs a background service which recursively scans the device file system for playable audio files starting at the root folder defined in settings. Ideally this scanning is triggered every time the app is opened and the track cache is updated every time an audio file is added or removed from the file system.

## Playlist Database

The playlist database is a persisted data store of playlists. Its structure is a document database structure as so…

    

```json
{
  "_id": "12345",
  "name": "Evening Chill",
  "description": "Relax and unwind with a curated list of lo-fi and ambient tracks.",
  "creation_date": "2025-09-23T12:00:00Z",
  "last_updated": "2025-09-23T12:00:00Z",
  "track_id_album_cover": "track-xyz987",
  "tracks": [
    {
      "track_id": "track-xyz987"
    },
    {
      "track_id": "track-abc456"
    },
    {
      "track_id": "track-def123"
    }
  ]
}

```

The track\_id field is a reference to an entry in the track cache.