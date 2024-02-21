package com.flowmosaic.calendar.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.ui.theme.getOnPrimaryColor
import com.flowmosaic.calendar.ui.theme.getPrimaryColor
import kotlinx.coroutines.launch

data class OnboardingPage(
    val imageRes: Int,
    val text: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    onFinish: (skipped: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = {
            pages.size
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getPrimaryColor())
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) { page ->
            OnboardingPageContent(index = page, page = pages[page])
        }

        Column(
            modifier = Modifier
        ) {
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
            )

            OnboardingNavigationButtons(pagerState = pagerState, pages = pages, onFinish = onFinish)
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingNavigationButtons(
    pagerState: PagerState,
    pages: List<OnboardingPage>,
    onFinish: (skipped: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val skipButtonColors = ButtonDefaults.buttonColors(
        containerColor = getPrimaryColor(),
        contentColor = getOnPrimaryColor()
    )

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = getOnPrimaryColor(),
        contentColor = getPrimaryColor()
    )

    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (pagerState.currentPage < pages.size - 1 && pagerState.currentPage > 0) {
            Button(onClick = { onFinish(true) }, colors = skipButtonColors) {
                Text(context.getString(R.string.onboarding_skip))
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp)) // Empty spacer for alignment
        }

        Row(modifier = Modifier) {
            if (pagerState.currentPage < pages.lastIndex) {
                Button(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }, colors = buttonColors) {
                    Text(context.getString(R.string.onboarding_next))
                }
            } else {
                Button(onClick = { onFinish(false) }, colors = buttonColors) {
                    Text(context.getString(R.string.onboarding_got_it))
                }
            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp), // This ensures the Row takes up the full width
        horizontalArrangement = Arrangement.Center, // This centers the content (dots) horizontally
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) {
            IndicatorSingleDot(isSelected = it == currentPage)
        }
    }
}

@Composable
fun IndicatorSingleDot(isSelected: Boolean) {
    val color =
        animateColorAsState(targetValue = if (isSelected) getOnPrimaryColor() else MaterialTheme.colorScheme.outline)

    val width = animateDpAsState(targetValue = if (isSelected) 35.dp else 15.dp, label = "")
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(15.dp)
            .width(width.value)
            .clip(CircleShape)
            .background(color.value)
    )
}

@Composable
fun OnboardingPageContent(index: Int, page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 32.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (index > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .padding(top = 24.dp, bottom = 24.dp)
                        .size(60.dp)
                        .border(2.dp, color = getOnPrimaryColor(), shape = CircleShape)
                ) {
                    Text(
                        text = "$index",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        color = getOnPrimaryColor(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = page.text,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = getOnPrimaryColor(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 32.dp)
                )
            } else {
                Text(
                    text = page.text,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = getOnPrimaryColor(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}