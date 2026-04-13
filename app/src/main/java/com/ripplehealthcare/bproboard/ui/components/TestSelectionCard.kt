package com.ripplehealthcare.bproboard.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.ripplehealthcare.bproboard.ui.theme.CardColor
import com.ripplehealthcare.bproboard.ui.theme.Green


@SuppressLint("NewApi")
@Composable
fun TestSelectionCard(title: String, imageResId: Int, handleClick: ()-> Unit, isCompleted:Boolean,enabled:Boolean) {
    // For GIFs, we use Coil's AsyncImage
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor, disabledContainerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCompleted) BorderStroke(1.dp, Green) else null,
        onClick = handleClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Use AsyncImage from Coil to load the GIF
            AsyncImage(
                model = imageResId, // Your R.drawable.your_gif_file
                contentDescription = title,
                imageLoader = imageLoader, // Use the ImageLoader with GIF support
                modifier = Modifier
                    .size(width = 100.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}